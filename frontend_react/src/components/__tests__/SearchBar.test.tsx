import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SearchBar } from '../SearchBar';

describe('SearchBar', () => {
  it('renders correctly', () => {
    render(<SearchBar onSearch={() => {}} />);
    expect(screen.getByPlaceholderText('Search...')).toBeInTheDocument();
  });

  it('calls onSearch with debounced value when typing', async () => {
    const onSearch = vi.fn();
    render(<SearchBar onSearch={onSearch} />);
    
    const searchInput = screen.getByPlaceholderText('Search...');
    await userEvent.type(searchInput, 'test');

    // Wait for debounce
    await new Promise((resolve) => setTimeout(resolve, 500));

    expect(onSearch).toHaveBeenCalledWith('test');
  });

  it('updates input value when typing', async () => {
    const onSearch = vi.fn();
    render(<SearchBar onSearch={onSearch} />);
    
    const searchInput = screen.getByPlaceholderText('Search...') as HTMLInputElement;
    await userEvent.type(searchInput, 'test');
    
    expect(searchInput.value).toBe('test');
  });

  it('clears input when clear button is clicked', async () => {
    const onSearch = vi.fn();
    render(<SearchBar onSearch={onSearch} />);
    
    const searchInput = screen.getByPlaceholderText('Search...') as HTMLInputElement;
    await userEvent.type(searchInput, 'test');
    
    const clearButton = screen.getByLabelText('Clear search');
    fireEvent.click(clearButton);

    expect(searchInput.value).toBe('');
    expect(onSearch).toHaveBeenCalledWith('');
  });
}); 