/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Manrope', 'Source Sans 3', 'system-ui', 'sans-serif'],
            },
            colors: {
                'apple-black': '#000000',
                'apple-blue': '#0071e3',
                'apple-gray-1': '#f5f5f7',
                'apple-gray-2': '#f0f0f0',
                'apple-gray-3': '#e8e8ed',
                'apple-gray-4': '#d2d2d7',
                'apple-gray-5': '#a1a1a6',
                'apple-gray-6': '#515154',
            },
            spacing: {
                'section': '3rem',
                'section-lg': '4rem',
                'section-xl': '6rem',
            },
            animation: {
                'fade-in': 'fadeIn 0.8s ease-out',
                'fade-up': 'fadeUp 0.8s ease-out',
                'fade-up-delay-1': 'fadeUp 0.8s ease-out 0.1s backwards',
                'fade-up-delay-2': 'fadeUp 0.8s ease-out 0.2s backwards',
                'fade-up-delay-3': 'fadeUp 0.8s ease-out 0.3s backwards',
                'fade-up-delay-4': 'fadeUp 0.8s ease-out 0.4s backwards',
            },
            keyframes: {
                fadeIn: {
                    '0%': { opacity: '0' },
                    '100%': { opacity: '1' }
                },
                fadeUp: {
                    '0%': { opacity: '0', transform: 'translateY(20px)' },
                    '100%': { opacity: '1', transform: 'translateY(0)' }
                },
            },
            transitionDuration: {
                '300': '300ms',
                '500': '500ms',
            },
        },
    },
    plugins: [],
}
