/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bili: {
          50: '#eff6ff',
          500: '#00a1d6',
          600: '#0091c2'
        }
      }
    }
  },
  plugins: []
};

