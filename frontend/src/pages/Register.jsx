import { Link } from 'react-router-dom'

export default function Register() {
    return (
        <div className="min-h-[70vh] flex items-center justify-center px-6">
            <div className="w-full max-w-sm">
                <div className="mb-8 text-center">
                    <h1 className="text-2xl font-semibold mb-2">Create an account</h1>
                    <p className="text-gray-500 text-sm">Join myShop to start exploring premium products</p>
                </div>

                <form className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Full Name</label>
                        <input
                            type="text"
                            className="w-full bg-[#0f0f0f] border border-gray-800 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            placeholder="John Doe"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Email address</label>
                        <input
                            type="email"
                            className="w-full bg-[#0f0f0f] border border-gray-800 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            placeholder="name@example.com"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1.5">Password</label>
                        <input
                            type="password"
                            className="w-full bg-[#0f0f0f] border border-gray-800 rounded-md px-3 py-2 text-white focus:outline-none focus:border-gray-500"
                            placeholder="••••••••"
                        />
                    </div>
                    <button type="button" className="w-full btn-primary mt-6">
                        Create Account
                    </button>
                </form>

                <p className="mt-6 text-center text-sm text-gray-500">
                    Already have an account? <Link to="/login" className="text-white hover:underline">Sign in</Link>
                </p>
            </div>
        </div>
    )
}
