import { useState, useEffect } from 'react';

export default function RateLimitModal({ rateLimit, onClose }) {
    const [timeLeft, setTimeLeft] = useState(0);

    useEffect(() => {
        if (rateLimit) {
            setTimeLeft(rateLimit.retryAfter);
        }
    }, [rateLimit]);

    useEffect(() => {
        if (timeLeft <= 0) {
            if (rateLimit) onClose(); // Auto-close when timer reaches 0
            return;
        }

        const timer = setInterval(() => {
            setTimeLeft(prev => prev - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [timeLeft, rateLimit, onClose]);

    if (!rateLimit) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <div className="bg-[#1a1a1a] border border-red-500/30 rounded-lg shadow-2xl max-w-md w-full p-6 text-center animate-in fade-in zoom-in duration-200">
                <div className="w-16 h-16 bg-red-500/10 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg className="w-8 h-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                </div>
                <h2 className="text-xl font-semibold text-white mb-2">Too Many Requests</h2>
                <p className="text-gray-400 mb-6">
                    {rateLimit.message || "Please slow down. You've exceeded the rate limit for this endpoint."}
                </p>

                <div className="bg-black/50 rounded-md py-4 mb-6 border border-gray-800">
                    <div className="text-sm text-gray-500 mb-1">Please wait before trying again</div>
                    <div className="text-3xl font-mono text-white flex items-center justify-center gap-2">
                        <span>{Math.floor(timeLeft / 60).toString().padStart(2, '0')}</span>
                        <span className="text-gray-600">:</span>
                        <span>{(timeLeft % 60).toString().padStart(2, '0')}</span>
                    </div>
                </div>

                <div className="text-xs text-gray-500 flex justify-center gap-1 items-center">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    Protected by Rate Limiting
                </div>
            </div>
        </div>
    );
}
