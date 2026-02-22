# mySHOP — Enterprise Event-Driven E-Commerce Infrastructure

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:0ea5e9,100:6366f1&height=220&section=header&text=mySHOP&fontSize=45&fontColor=ffffff&animation=fadeIn&fontAlignY=38" />
</p>

---

<h2 align="center">Overview</h2>

A high-performance e-commerce platform engineered with a focus on event-driven architecture, distributed caching, and scalable service decoupling.

---

<h2 align="center">Technical Architecture</h2>

The system utilizes a containerized stack designed for low-latency data retrieval and resilient asynchronous processing.

<br>

<table align="center">
<tr>
<th>Tier</th>
<th>Technology</th>
</tr>
<tr>
<td>Backend</td>
<td>Java 21, Spring Boot 3, Spring Security (JWT)</td>
</tr>
<tr>
<td>Data</td>
<td>PostgreSQL, Redis, MongoDB</td>
</tr>
<tr>
<td>Messaging</td>
<td>Apache Kafka</td>
</tr>
<tr>
<td>Frontend</td>
<td>React 18, Vite, Tailwind CSS, Zustand</td>
</tr>
<tr>
<td>Ops</td>
<td>Docker, Docker Compose</td>
</tr>
</table>

---

<h2 align="center">Core Capabilities</h2>

### Asynchronous Event Processing

The platform decouples primary transactions from side-effects using Apache Kafka. Events such as order placement and cancellations are processed by isolated consumers to handle notifications and telemetry without impacting user response times.

### Tiered Caching Strategy

Integrated Redis caching on high-traffic catalog endpoints minimizes database load. The system implements automated cache eviction policies to maintain data consistency across administrative updates.

### Traffic Governance

Sophisticated rate-limiting infrastructure (Bucket4j) protects internal APIs from saturation. The system includes synchronized frontend middleware to manage user experience during high-concurrency periods.

### Secure State Management

Implements stateless authentication via JWT and strictly enforced role-based access control (RBAC) for administrative governance of products, cache states, and event streams.

---

<h2 align="center">Deployment</h2>

Ensure Docker and Docker Compose are installed on the host system.

```bash
git clone https://github.com/medhxnsh/mySHOP.git
cd mySHOP
docker-compose up -d --build
