/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#F7F3EC',
        ink: { DEFAULT: '#1C1917', soft: '#57534E', faint: '#A8A29E' },
        pine: { DEFAULT: '#0B5D4B', dark: '#084538', soft: '#E3F0EC' },
        turmeric: { DEFAULT: '#E8A317', soft: '#FDF3D7' },
        chili: { DEFAULT: '#B42318', soft: '#FDECEA' },
        line: '#E7E0D3',
      },
      fontFamily: {
        display: ['Fraunces', 'Georgia', 'serif'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
