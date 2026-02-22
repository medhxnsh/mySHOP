import { useState, useEffect } from 'react';
import axios from 'axios';
import { Activity, Server } from 'lucide-react';

export default function AdminKafka() {
    const [topics, setTopics] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchKafkaData = async () => {
        try {
            // In a real app, this would hit our Spring Boot admin endpoint 
            // which then fetches from Kafka's AdminClient.
            // For Phase 5, we simulate the dashboard or proxy to Kafka-UI.
            // Since we have provectuslabs/kafka-ui running on 8081:
            setTopics([
                { name: 'order.placed', partitions: 3, replicas: 1, status: 'Healthy', consumerLag: 0 },
                { name: 'order.status.updated', partitions: 3, replicas: 1, status: 'Healthy', consumerLag: 0 },
                { name: 'inventory.updated', partitions: 3, replicas: 1, status: 'Healthy', consumerLag: 0 },
                { name: 'myshop.dlt', partitions: 1, replicas: 1, status: 'Healthy', consumerLag: 0 }
            ]);
            setLoading(false);
        } catch (err) {
            console.error('Failed to fetch Kafka topics:', err);
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchKafkaData();
        const interval = setInterval(fetchKafkaData, 10000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="max-w-7xl mx-auto px-6 py-12">
            <div className="flex justify-between items-center mb-10 text-white">
                <div>
                    <h1 className="text-3xl font-semibold mb-2 flex items-center gap-3">
                        <Server className="text-blue-500" />
                        Kafka Dashboard
                    </h1>
                    <p className="text-gray-400">KRaft Mode Topic & Consumer Group Monitor</p>
                </div>
                <a
                    href="http://localhost:8081"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-colors"
                >
                    Open Full Kafka UI
                </a>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10 text-white">
                <div className="bg-[#121212] border border-gray-800 rounded-xl p-6">
                    <div className="text-gray-400 text-sm mb-1">Cluster State</div>
                    <div className="text-2xl font-semibold text-emerald-400 flex items-center gap-2">
                        <Activity size={20} /> Healthy
                    </div>
                </div>
                <div className="bg-[#121212] border border-gray-800 rounded-xl p-6">
                    <div className="text-gray-400 text-sm mb-1">Active Brokers</div>
                    <div className="text-2xl font-semibold">1 (KRaft)</div>
                </div>
                <div className="bg-[#121212] border border-gray-800 rounded-xl p-6">
                    <div className="text-gray-400 text-sm mb-1">Total Topics</div>
                    <div className="text-2xl font-semibold">{topics.length}</div>
                </div>
                <div className="bg-[#121212] border border-gray-800 rounded-xl p-6">
                    <div className="text-gray-400 text-sm mb-1">Consumer Groups</div>
                    <div className="text-2xl font-semibold">3 Active</div>
                </div>
            </div>

            <div className="bg-[#121212] border border-gray-800 rounded-xl overflow-hidden">
                <div className="p-6 border-b border-gray-800">
                    <h2 className="text-xl font-medium text-white">Registered Topics</h2>
                </div>
                <div className="overflow-x-auto text-white">
                    <table className="w-full text-left flex-col text-sm text-gray-400">
                        <thead className="text-xs uppercase bg-[#1a1a1a] text-gray-500 border-b border-gray-800">
                            <tr>
                                <th className="px-6 py-4">Topic Name</th>
                                <th className="px-6 py-4">Partitions</th>
                                <th className="px-6 py-4">Replicas</th>
                                <th className="px-6 py-4">Status</th>
                                <th className="px-6 py-4">Consumer Lag</th>
                            </tr>
                        </thead>
                        <tbody>
                            {topics.map(topic => (
                                <tr key={topic.name} className="border-b border-gray-800 hover:bg-[#1a1a1a] transition-colors">
                                    <td className="px-6 py-4 font-medium text-blue-400">
                                        {topic.name === 'myshop.dlt' ? <span className="text-red-400 flex items-center gap-2">{topic.name}</span> : topic.name}
                                    </td>
                                    <td className="px-6 py-4 text-white">{topic.partitions}</td>
                                    <td className="px-6 py-4 text-white">{topic.replicas}</td>
                                    <td className="px-6 py-4">
                                        <span className="bg-emerald-500/10 text-emerald-400 px-2.5 py-1 rounded-full text-xs font-medium border border-emerald-500/20">
                                            {topic.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-white font-mono">{topic.consumerLag} msgs</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
