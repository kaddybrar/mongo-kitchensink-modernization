import React from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { performance, PerformanceObserver } from 'perf_hooks';
import MemberList from '../../src/components/MemberList';

// Create a new QueryClient
const queryClient = new QueryClient();

// Set up performance observer
const obs = new PerformanceObserver((list) => {
  const entries = list.getEntries();
  entries.forEach((entry) => {
    console.log(`Component: ${entry.name}, Duration: ${entry.duration}ms`);
  });
});
obs.observe({ entryTypes: ['measure'] });

// Measure initial render
performance.mark('render-start');

const container = document.createElement('div');
const root = createRoot(container);

root.render(
  <QueryClientProvider client={queryClient}>
    <MemberList />
  </QueryClientProvider>
);

performance.mark('render-end');
performance.measure('Initial Render', 'render-start', 'render-end');

// Measure component updates
setTimeout(async () => {
  performance.mark('update-start');
  
  // Trigger a re-render
  root.render(
    <QueryClientProvider client={queryClient}>
      <MemberList key="update" />
    </QueryClientProvider>
  );

  performance.mark('update-end');
  performance.measure('Component Update', 'update-start', 'update-end');

  // Log memory usage
  const memoryUsage = process.memoryUsage();
  console.log('Memory Usage:', {
    heapTotal: `${Math.round(memoryUsage.heapTotal / 1024 / 1024)}MB`,
    heapUsed: `${Math.round(memoryUsage.heapUsed / 1024 / 1024)}MB`,
    external: `${Math.round(memoryUsage.external / 1024 / 1024)}MB`,
    rss: `${Math.round(memoryUsage.rss / 1024 / 1024)}MB`,
  });

  // Clean up
  setTimeout(() => {
    root.unmount();
    process.exit(0);
  }, 1000);
}, 2000); 