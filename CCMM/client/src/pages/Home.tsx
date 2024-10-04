// Code: Home Page Component
import ccmm from "@/images/ccmm.png"

function HomePage() {
    return (
        <div className="flex min-h-screen flex-col items-center justify-center gap-16">
            <div className="w-full text-white bg-gradient-to-r from-green-400 to-green-900">
                <div className="flex min-h-screen flex-col items-center justify-center gap-16">
                    {/* Hero Section */}
                    <div className="my-12 flex h-full min-w-full flex-col items-center justify-evenly sm:flex-row sm:pt-0">
                    <div className="glass-effect flex w-full flex-col items-center text-center sm:w-1/2 p-8">
                        <h1 className="text-4xl font-bold text-white">
                        Simplify Your Code with Our Complexity Measuring Tool
                        </h1>
                        <p className="mt-4 text-lg text-gray-200">
                        Automatically analyze your code for complexity, optimize performance, and enhance maintainability.
                        </p>
                        <button className="mt-8 rounded-lg bg-[#425e33] px-6 py-3 text-white font-bold hover:bg-[#314626]">
                        Get Started
                        </button>
                    </div>
                    <div className="flex w-full items-center justify-center sm:w-1/2">
                        <img
                        src={ccmm}
                        alt="Code complexity illustration"
                        className="w-full max-w-lg drop-shadow-lg"
                        />
                    </div>
                    </div>
                </div>
            </div>


            {/* Features Section */}
            <div className="my-12 flex w-full flex-col items-center">
                <h2 className="text-3xl font-semibold text-white">Features</h2>
                <div className="mt-8 grid w-full max-w-4xl grid-cols-1 gap-8 sm:grid-cols-2">
                    <div className="rounded-lg border border-gray-700 bg-gray-800 p-6 text-center">
                        <h3 className="text-xl font-semibold text-white">
                            Real-Time Analysis
                        </h3>
                        <p className="mt-2 text-gray-300">
                            Get instant feedback on your code complexity and
                            identify bottlenecks.
                        </p>
                    </div>
                    <div className="rounded-lg border border-gray-700 bg-gray-800 p-6 text-center">
                        <h3 className="text-xl font-semibold text-white">
                            Detailed Reports
                        </h3>
                        <p className="mt-2 text-gray-300">
                            Receive comprehensive reports with suggestions for
                            optimization.
                        </p>
                    </div>
                    <div className="rounded-lg border border-gray-700 bg-gray-800 p-6 text-center">
                        <h3 className="text-xl font-semibold text-white">
                            Easy Integration
                        </h3>
                        <p className="mt-2 text-gray-300">
                            Seamlessly integrate with popular code editors and
                            CI/CD pipelines.
                        </p>
                    </div>
                    <div className="rounded-lg border border-gray-700 bg-gray-800 p-6 text-center">
                        <h3 className="text-xl font-semibold text-white">
                            Cross-Language Support
                        </h3>
                        <p className="mt-2 text-gray-300">
                            Analyze code complexity across multiple programming
                            languages.
                        </p>
                    </div>
                </div>
            </div>

            {/* Testimonials Section */}
            <div className="my-12 flex w-full flex-col items-center text-center">
                <h2 className="text-3xl font-semibold text-white">
                    What Our Users Say
                </h2>
                <div className="mt-8 flex w-full max-w-4xl flex-col gap-8 sm:flex-row">
                    <div className="rounded-lg border border-gray-700 bg-gray-800 p-6 text-white shadow-lg">
                        <p className="italic text-gray-300">
                            "This tool has significantly improved our code
                            review process and helped us keep our codebase clean
                            and maintainable."
                        </p>
                        <p className="mt-4 text-sm font-semibold">
                            - Jane Doe, Lead Developer
                        </p>
                    </div>
                    <div className="rounded-lg border border-gray-700 bg-gray-800 p-6 text-white shadow-lg">
                        <p className="italic text-gray-300">
                            "The real-time analysis feature saves us hours
                            during development and makes it easy to catch issues
                            early."
                        </p>
                        <p className="mt-4 text-sm font-semibold">
                            - John Smith, Software Engineer
                        </p>
                    </div>
                </div>
            </div>

            {/* Call-to-Action Section */}
            <div className="my-12 flex w-full flex-col items-center text-center">
                <h2 className="text-3xl font-semibold text-white">
                    Ready to Improve Your Code?
                </h2>
                <p className="mt-4 text-lg text-gray-300">
                    Sign up today and start optimizing your code with ease.
                </p>
                <button className="mt-8 rounded-lg bg-[#648f4d] px-6 py-3 text-white hover:bg-[#3f5b30]">
                    Start Free Trial
                </button>
            </div>
        </div>
    )
}

export default HomePage
