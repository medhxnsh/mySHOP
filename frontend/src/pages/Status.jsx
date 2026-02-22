import { FileText, Activity, Server, Monitor } from 'lucide-react'

const phases = [
    { phase: 0, name: 'Project Scaffold', status: 'Complete', desc: 'Spring Boot skeleton, PostgreSQL schema, Docker, React frontend' },
    { phase: 1, name: 'Products & Auth', status: 'Complete', desc: 'JWT authentication, product catalog, JPA entities, MapStruct, @Async' },
    { phase: 2, name: 'Orders & Transactions', status: 'Complete', desc: 'Shopping cart, order placement, @Transactional, CompletableFuture' },
    { phase: 3, name: 'MongoDB: Reviews', status: 'Complete', desc: 'Polyglot persistence, document model, TTL indexes' },
    { phase: 4, name: 'Redis: Caching', status: 'Complete', desc: 'Cache-aside, rate limiting, distributed locks' },
    { phase: 5, name: 'Kafka: Events', status: 'Complete', desc: 'KRaft mode, event-driven, dead letter topics' },
    { phase: 6, name: 'Elasticsearch', status: 'Pending', desc: 'Full-text search, fuzzy matching, faceted filters' },
    { phase: 7, name: 'Production & Azure', status: 'Pending', desc: 'Observability, CI/CD, cloud deployment' },
]

const techStack = [
    { name: 'Spring Boot 3.x', desc: 'Backend framework' },
    { name: 'PostgreSQL 16', desc: 'Primary database' },
    { name: 'Flyway', desc: 'DB migrations' },
    { name: 'React 18', desc: 'Frontend UI' },
    { name: 'MongoDB 7', desc: 'Document store' },
    { name: 'Redis 7', desc: 'Cache & locks' },
    { name: 'Apache Kafka', desc: 'Event streaming' },
    { name: 'Elasticsearch 8', desc: 'Search engine' },
]

function StatusBadge({ status }) {
    if (status === 'Complete') {
        return <span className="text-xs font-medium text-gray-400 bg-gray-900 border border-gray-800 px-2 py-0.5 rounded-full">Complete</span>
    }
    if (status === 'Active') {
        return <span className="text-xs font-medium text-white bg-gray-800 border border-gray-600 px-2 py-0.5 rounded-full">Active</span>
    }
    return <span className="text-xs font-medium text-gray-600 border border-gray-800/50 px-2 py-0.5 rounded-full">Pending</span>
}

export default function Status() {
    return (
        <div className="max-w-4xl mx-auto px-6 py-24">
            {/* Hero Section */}
            <div className="mb-24 animate-fade-in">
                <h1 className="text-4xl font-semibold mb-4 tracking-tight">System Status</h1>
                <p className="text-lg text-gray-400 max-w-2xl leading-relaxed mb-8">
                    Production e-commerce API. Spring Boot 3 · PostgreSQL · Redis · Kafka · Elasticsearch.
                </p>
                <div className="flex flex-wrap gap-4">
                    <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer" className="btn-secondary flex items-center gap-2 text-sm">
                        <FileText size={16} /> API Documentation
                    </a>
                    <a href="http://localhost:8080/actuator/health" target="_blank" rel="noreferrer" className="btn-secondary flex items-center gap-2 text-sm">
                        <Activity size={16} /> Health Status
                    </a>
                    <a href="http://localhost:8080" target="_blank" rel="noreferrer" className="btn-secondary flex items-center gap-2 text-sm">
                        <Server size={16} /> Backend
                    </a>
                </div>
            </div>

            {/* Phase Roadmap */}
            <section className="mb-24 animate-fade-in" style={{ animationDelay: '100ms' }}>
                <h2 className="text-xl font-medium mb-8">Build Roadmap</h2>
                <div className="flex flex-col gap-4">
                    {phases.map((p) => (
                        <div key={p.phase} className={`flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-lg border border-gray-800 transition-colors bg-[#0f0f0f] ${p.status === 'Pending' ? 'opacity-50' : ''}`}>
                            <div className="flex items-start gap-4 mb-3 sm:mb-0">
                                <div className="text-gray-500 font-mono text-sm mt-0.5">{String(p.phase).padStart(2, '0')}</div>
                                <div>
                                    <div className="font-medium text-gray-200 text-sm">{p.name}</div>
                                    <div className="text-gray-500 text-xs mt-1">{p.desc}</div>
                                </div>
                            </div>
                            <div className="self-start sm:self-auto ml-8 sm:ml-0">
                                <StatusBadge status={p.status} />
                            </div>
                        </div>
                    ))}
                </div>
            </section>

            {/* Tech Stack */}
            <section className="mb-24 animate-fade-in" style={{ animationDelay: '150ms' }}>
                <h2 className="text-xl font-medium mb-8">Stack</h2>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    {techStack.map((t) => (
                        <div key={t.name} className="p-4 rounded-lg border border-gray-800 bg-[#0f0f0f]">
                            <div className="font-medium text-gray-300 text-sm mb-1">{t.name}</div>
                            <div className="text-gray-500 text-xs">{t.desc}</div>
                        </div>
                    ))}
                </div>
            </section>

            <section className="pt-8 border-t border-gray-800 animate-fade-in" style={{ animationDelay: '200ms' }}>
                <div className="flex items-center gap-2 text-sm text-gray-400">
                    <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                    All systems operational
                </div>
            </section>
        </div>
    )
}
