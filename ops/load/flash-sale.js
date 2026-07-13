// k6 load test — flash-sale hot path (Phase 9).
//
// Scenario: BUYERS distinct authenticated users burst-buy a STOCK-unit sale.
// Invariants asserted by thresholds:
//   - exactly STOCK purchases accepted (zero oversell, zero undersell)
//   - everyone else gets a clean SOLD_OUT business rejection
//   - hot-path latency p99 < 300ms under full contention
//
// Run via run-flash-sale-test.sh (seeds users, raises auth rate limits,
// creates + activates the sale, verifies DB/Redis accounting afterwards).

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const BUYERS = parseInt(__ENV.BUYERS || '500');
const STOCK = parseInt(__ENV.STOCK || '300');
const SALE_ID = __ENV.SALE_ID; // created by the wrapper script
const PASSWORD = 'LoadTest@123';

const accepted = new Counter('flash_accepted');
const soldOut = new Counter('flash_sold_out');
const otherFailures = new Counter('flash_other_failures');
const purchaseLatency = new Trend('flash_purchase_latency', true);

export const options = {
    scenarios: {
        burst: {
            executor: 'per-vu-iterations',
            vus: BUYERS,
            iterations: 1, // one purchase attempt per user — like a real drop
            maxDuration: '2m',
        },
    },
    thresholds: {
        flash_purchase_latency: ['p(99)<300'],
        flash_accepted: [`count==${STOCK}`],
        flash_other_failures: ['count==0'],
        checks: ['rate>0.99'],
    },
};

export function setup() {
    // Log every buyer in up front (auth limit is raised by the wrapper).
    const tokens = [];
    const batchSize = 50;
    for (let i = 0; i < BUYERS; i += batchSize) {
        const reqs = [];
        for (let j = i; j < Math.min(i + batchSize, BUYERS); j++) {
            reqs.push(['POST', `${BASE}/api/v1/auth/login`,
                JSON.stringify({ email: `loadtest${j}@example.com`, password: PASSWORD }),
                { headers: { 'Content-Type': 'application/json' } }]);
        }
        const responses = http.batch(reqs);
        for (const r of responses) {
            if (r.status !== 200) {
                throw new Error(`setup login failed: ${r.status} ${r.body}`);
            }
            tokens.push(r.json('data.accessToken'));
        }
    }
    console.log(`setup complete: ${tokens.length} buyers authenticated, sale ${SALE_ID}, stock ${STOCK}`);
    return { tokens };
}

export default function (data) {
    const token = data.tokens[__VU - 1];
    const res = http.post(`${BASE}/api/v1/flash-sales/${SALE_ID}/purchase`, null, {
        headers: { Authorization: `Bearer ${token}` },
        tags: { name: 'flash_purchase' },
    });
    purchaseLatency.add(res.timings.duration);

    if (res.status === 202) {
        accepted.add(1);
        check(res, { 'accepted has reservationId': (r) => r.json('data.reservationId') !== undefined });
    } else if (res.status === 422 && res.json('error.code') === 'FLASH_SALE_SOLD_OUT') {
        soldOut.add(1);
        check(res, { 'sold out is clean 422': () => true });
    } else {
        otherFailures.add(1);
        console.error(`unexpected: ${res.status} ${res.body}`);
    }
}
