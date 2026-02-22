import { useState } from 'react';
import axios from 'axios';

export default function FlashSale() {
    const [logs, setLogs] = useState([]);
    const [isSimulating, setIsSimulating] = useState(false);

    const addLog = (message, type = 'info') => {
        setLogs(prev => [{ id: Date.now() + Math.random(), message, type, time: new Date().toLocaleTimeString() }, ...prev]);
    };

    const runSimulation = async () => {
        setIsSimulating(true);
        setLogs([]);
        addLog('Starting High-Contention Flash Sale Simulation...', 'info');

        // Note: For this to work, we need an item with ID in DB, or just use an invalid one to see the 404/lock attempt.
        // We will just send 10 concurrent requests to place an order.
        const orderPayload = {
            items: [
                { productId: '11111111-1111-1111-1111-111111111111', quantity: 1 } // Dummy ID or replace with a real one
            ],
            shippingAddress: {
                street: "123 Flash St",
                city: "Speed",
                state: "CA",
                country: "USA",
                zipCode: "90001"
            },
            paymentMethod: "CREDIT_CARD"
        };

        const promises = [];
        for (let i = 0; i < 5; i++) {
            promises.push(
                axios.post('/api/v1/orders', orderPayload)
                    .then(() => addLog(`User ${i + 1}: Order placed successfully!`, 'success'))
                    .catch(err => {
                        const msg = err.response?.data?.error?.message || err.message;
                        addLog(`User ${i + 1}: Failed - ${msg}`, 'error');
                    })
            );
        }

        await Promise.all(promises);
        addLog('Simulation Complete.', 'info');
        setIsSimulating(false);
    };

    return (
        <div className="max-w-4xl mx-auto px-6 py-12">
            <h1 className="text-3xl font-semibold mb-4 text-orange-500">⚡ Flash Sale Demo</h1>
            <p className="text-gray-400 mb-8">
                This page simulates high-contention checkout where multiple users try to buy the same limited-stock item at the exact same moment.
                View the logs to see how <strong>Redis Distributed Locks</strong> handle the concurrency!
            </p>

            <button
                onClick={runSimulation}
                disabled={isSimulating}
                className="w-full bg-orange-600 hover:bg-orange-500 text-white font-bold py-4 rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed mb-8"
            >
                {isSimulating ? 'Simulating Traffic...' : 'Trigger Concurrent Checkouts (5 Users)'}
            </button>

            <div className="bg-[#0a0a0a] border border-gray-800 rounded-lg p-4 h-96 overflow-y-auto font-mono text-sm shadow-inner">
                {logs.length === 0 ? (
                    <div className="text-gray-600 h-full flex items-center justify-center">Waiting for simulation to start...</div>
                ) : (
                    <div className="space-y-2">
                        {logs.map(log => (
                            <div key={log.id} className="flex gap-4 border-b border-gray-900/50 pb-2">
                                <span className="text-gray-600 shrink-0">[{log.time}]</span>
                                <span className={`${log.type === 'error' ? 'text-red-400' :
                                        log.type === 'success' ? 'text-green-400' :
                                            'text-blue-400'
                                    }`}>
                                    {log.message}
                                </span>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
