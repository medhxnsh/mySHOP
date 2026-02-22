# mySHOP — Enterprise Event-Driven E-Commerce Infrastructure

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:0ea5e9,100:6366f1&height=220&section=header&text=mySHOP&fontSize=45&fontColor=ffffff&animation=fadeIn&fontAlignY=38" />
</p>

---

<h3 align="center">Overview</h3>

<p align="center">
A high-performance e-commerce platform engineered with a focus on event-driven architecture, distributed caching, and scalable service decoupling.
</p>

---

<h3 align="center">Technical Architecture</h3>

<p align="center">
The system utilizes a containerized stack designed for low-latency data retrieval and resilient asynchronous processing.
</p>

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

<h3 align="center">Core Capabilities</h3>

<h4 align="center">Asynchronous Event Processing</h4>

<p align="center">
The platform decouples primary transactions from side-effects using Apache Kafka. Events such as order placement and cancellations are processed by isolated consumers to handle notifications and telemetry without impacting user response times.
</p>

<h4 align="center">Tiered Caching Strategy</h4>

<p align="center">
Integrated Redis caching on high-traffic catalog endpoints minimizes database load. The system implements automated cache eviction policies to maintain data consistency across administrative updates.
</p>

<h4 align="center">Traffic Governance</h4>

<p align="center">
Sophisticated rate-limiting infrastructure (Bucket4j) protects internal APIs from saturation. The system includes synchronized frontend middleware to manage user experience during high-concurrency periods.
</p>

<h4 align="center">Secure State Management</h4>

<p align="center">
Implements stateless authentication via JWT and strictly enforced role-based access control (RBAC) for administrative governance of products, cache states, and event streams.
</p>

---

<h3 align="center">Deployment</h3>

<p align="center">
Ensure Docker and Docker Compose are installed on the host system.
</p>

```bash
git clone https://github.com/medhxnsh/mySHOP.git
cd mySHOP
docker-compose up -d --build
