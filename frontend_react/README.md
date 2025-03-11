# Kitchensink React Frontend

A modern React frontend for the Kitchensink application, built with TypeScript, React Query, and Tailwind CSS.

## Tech Stack

- React 18
- TypeScript
- Vite
- React Query (TanStack Query)
- Tailwind CSS
- Headless UI
- Heroicons
- Axios
- Jest & React Testing Library
- ESLint & Prettier

## Features

- 🎯 Modern React with TypeScript
- 🚀 Fast development with Vite
- 📱 Responsive design
- 🎨 Beautiful UI with Tailwind CSS
- 🔄 Efficient data fetching with React Query
- 📝 Form validation
- 🔍 Search functionality
- 📊 Pagination
- 🧪 Comprehensive testing setup
- 📏 Code quality tools (ESLint, Prettier)

## Getting Started

### Prerequisites

- Node.js 18 or later
- npm or yarn
- Docker (optional, for containerized development)

### Installation

```bash
# Clone the repository (if not already done)
git clone https://github.com/yourusername/mongo-kitchensink-modernization.git
cd mongo-kitchensink-modernization/frontend_react

# Install dependencies
npm install

# Start development server
npm run dev
```

### Development with Docker

```bash
# From the root directory
docker compose up -d frontend-new
```

The application will be available at http://localhost:3001

## Available Scripts

```bash
# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests
npm run test

# Run tests with coverage
npm run test:coverage

# Run tests with UI
npm run test:ui

# Type checking
npm run type-check

# Lint code
npm run lint

# Format code
npm run format
```

## Project Structure

```
frontend_react/
├── src/
│   ├── components/     # React components
│   ├── hooks/         # Custom hooks
│   ├── services/      # API services
│   ├── types/         # TypeScript types/interfaces
│   ├── utils/         # Utility functions
│   ├── App.tsx        # Root component
│   ├── main.tsx       # Entry point
│   └── index.css      # Global styles
├── public/            # Static assets
├── tests/            # Test files
├── vite.config.ts    # Vite configuration
├── tailwind.config.js # Tailwind configuration
├── postcss.config.js # PostCSS configuration
└── package.json      # Project dependencies and scripts
```

## Key Components

### MemberList

The main component that displays the list of members with features like:
- Pagination
- Search
- Add/Edit/Delete operations
- Responsive table layout

### MemberForm

A reusable form component for adding and editing members with:
- Form validation
- Error handling
- Loading states
- Modal dialog

### SearchBar

A reusable search component with:
- Debounced input
- Clear functionality
- Responsive design

## API Integration

The application uses Axios for API communication with the following features:
- Base URL configuration
- Request/Response interceptors
- Error handling
- Type-safe responses

## State Management

- React Query for server state management
- Local state with React hooks
- Optimistic updates for better UX

## Styling

The application uses Tailwind CSS with:
- Custom theme configuration
- Responsive design
- Dark mode support (planned)
- Custom component classes
- Form styling with @tailwindcss/forms

## Testing

Comprehensive testing setup with:
- Jest for unit tests
- React Testing Library for component tests
- Vitest for fast test execution
- Coverage reporting

## Code Quality

- ESLint for code linting
- Prettier for code formatting
- TypeScript for type safety
- Husky for pre-commit hooks (planned)

## Environment Variables

```env
# API Configuration
VITE_API_URL=http://localhost:8081

# Feature Flags
VITE_ENABLE_DARK_MODE=false
VITE_ENABLE_ANALYTICS=false
```

## Docker Support

The application includes Docker support with:
- Multi-stage builds
- Development and production configurations
- Environment variable handling
- Health checks

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.
