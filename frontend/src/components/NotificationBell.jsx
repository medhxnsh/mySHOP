import { useState, useEffect, useRef } from 'react';
import { Bell } from 'lucide-react';
import axios from 'axios';
import toast, { Toaster } from 'react-hot-toast';
import useAuthStore from '../store/authStore';

export default function NotificationBell() {
    const [notifications, setNotifications] = useState([]);
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);
    const token = localStorage.getItem('token');

    // Polling or fetch on mount
    const fetchNotifications = async () => {
        if (!token) return;
        try {
            const res = await axios.get('/api/v1/notifications', {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (res.data?.success) {
                const newNotifs = res.data.data.content || [];

                // Check if there's a new notification we haven't seen in state yet
                setNotifications(prev => {
                    if (prev.length > 0 && newNotifs.length > 0) {
                        const prevLatestId = prev[0]?.id;
                        const currLatestId = newNotifs[0]?.id;

                        // Show toast if a new unread arrives
                        if (prevLatestId !== currLatestId && !newNotifs[0].isRead) {
                            toast.success(`New Notification: ${newNotifs[0].title}`);
                        }
                    }
                    return newNotifs;
                });
            }
        } catch (err) {
            console.error('Failed to fetch notifications', err);
        }
    };

    useEffect(() => {
        fetchNotifications();
        // Polling every 15s for Phase 5
        const interval = setInterval(fetchNotifications, 15000);
        return () => clearInterval(interval);
    }, [token]);

    // Close on outside click
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const markAllAsRead = async () => {
        try {
            await axios.put('/api/v1/notifications/read-all', {}, {
                headers: { Authorization: `Bearer ${token}` }
            });
            // instantly update UI
            setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
            setIsOpen(false);
        } catch (err) {
            console.error('Failed to mark all as read', err);
        }
    };

    if (!token) return null;

    const unreadCount = notifications.filter(n => !n.isRead).length;
    const top5 = notifications.slice(0, 5);

    return (
        <div className="relative" ref={dropdownRef}>
            <Toaster position="top-right" toastOptions={{ style: { background: '#333', color: '#fff' } }} />
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="relative p-2 text-gray-300 hover:text-white transition-colors focus:outline-none"
            >
                <Bell size={20} />
                {unreadCount > 0 && (
                    <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full animate-pulse shadow-[0_0_8px_rgba(239,68,68,0.8)]"></span>
                )}
            </button>

            {isOpen && (
                <div className="absolute right-0 mt-2 w-80 bg-[#121212] border border-gray-800 rounded-lg shadow-2xl z-50 overflow-hidden backdrop-blur-xl">
                    <div className="p-4 border-b border-gray-800 flex justify-between items-center bg-[#1a1a1a]">
                        <h3 className="font-semibold text-white">Notifications</h3>
                        {unreadCount > 0 && (
                            <button
                                onClick={markAllAsRead}
                                className="text-xs text-blue-400 hover:text-blue-300 font-medium transition-colors"
                            >
                                Mark all as read
                            </button>
                        )}
                    </div>

                    <div className="max-h-96 overflow-y-auto">
                        {top5.length === 0 ? (
                            <div className="p-6 text-center text-gray-500 text-sm">
                                No notifications yet.
                            </div>
                        ) : (
                            <div className="divide-y divide-gray-800/50">
                                {top5.map((notif) => (
                                    <div
                                        key={notif.id}
                                        className={`p-4 transition-colors hover:bg-[#1a1a1a] ${!notif.isRead ? 'bg-[#2563eb]/5' : ''
                                            }`}
                                    >
                                        <div className="flex justify-between items-start mb-1">
                                            <h4 className={`text-sm font-medium ${!notif.isRead ? 'text-white' : 'text-gray-300'}`}>
                                                {notif.title}
                                            </h4>
                                            {!notif.isRead && (
                                                <span className="w-1.5 h-1.5 bg-[#2563eb] rounded-full mt-1.5"></span>
                                            )}
                                        </div>
                                        <p className="text-xs text-gray-400 line-clamp-2">
                                            {notif.body}
                                        </p>
                                        <div className="mt-2 text-[10px] text-gray-600 uppercase tracking-wider">
                                            {new Date(notif.createdAt).toLocaleDateString()}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                    {notifications.length > 5 && (
                        <div className="p-3 border-t border-gray-800 text-center bg-[#1a1a1a]">
                            <button className="text-xs text-gray-400 hover:text-white transition-colors">
                                View all notifications
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
