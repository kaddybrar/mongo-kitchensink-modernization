import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemberList } from '../MemberList';
import { useMember } from '../../hooks/useMember';

// Mock the useMember hook
vi.mock('../../hooks/useMember', () => ({
  useMember: vi.fn(),
}));

const mockMembers = [
  { id: 1, name: 'John Doe', email: 'john@example.com', phoneNumber: '123-456-7890' },
  { id: 2, name: 'Jane Smith', email: 'jane@example.com', phoneNumber: '098-765-4321' },
];

describe('MemberList', () => {
  beforeEach(() => {
    (useMember as any).mockReturnValue({
      members: { data: mockMembers, isLoading: false, error: null },
      searchMembers: () => ({ data: mockMembers, isLoading: false, error: null }),
      deleteMember: { mutate: vi.fn() },
    });
  });

  it('renders member list correctly', () => {
    render(<MemberList />);
    
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Search members...')).toBeInTheDocument();
  });

  it('shows loading state', () => {
    (useMember as any).mockReturnValue({
      members: { data: [], isLoading: true, error: null },
      searchMembers: () => ({ data: [], isLoading: true, error: null }),
      deleteMember: { mutate: vi.fn() },
    });

    render(<MemberList />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('shows error state', () => {
    const errorMessage = 'Failed to fetch members';
    (useMember as any).mockReturnValue({
      members: { data: [], isLoading: false, error: new Error(errorMessage) },
      searchMembers: () => ({ data: [], isLoading: false, error: new Error(errorMessage) }),
      deleteMember: { mutate: vi.fn() },
    });

    render(<MemberList />);
    expect(screen.getByText(`Error loading members: ${errorMessage}`)).toBeInTheDocument();
  });

  it('handles search input', async () => {
    const searchMembers = vi.fn().mockReturnValue({
      data: mockMembers,
      isLoading: false,
      error: null,
    });
    
    (useMember as any).mockReturnValue({
      members: { data: mockMembers, isLoading: false, error: null },
      searchMembers,
      deleteMember: { mutate: vi.fn() },
    });

    render(<MemberList />);
    
    const searchInput = screen.getByPlaceholderText('Search members...');
    await userEvent.type(searchInput, 'John');

    // Wait for debounce
    await waitFor(() => {
      expect(searchMembers).toHaveBeenCalledWith('John');
    });
  });

  it('handles pagination', () => {
    const manyMembers = Array.from({ length: 15 }, (_, i) => ({
      id: i + 1,
      name: `User ${i + 1}`,
      email: `user${i + 1}@example.com`,
      phoneNumber: `123-456-${i + 1}`,
    }));

    (useMember as any).mockReturnValue({
      members: { data: manyMembers, isLoading: false, error: null },
      searchMembers: () => ({ data: manyMembers, isLoading: false, error: null }),
      deleteMember: { mutate: vi.fn() },
    });

    render(<MemberList />);
    
    // Should show pagination when there are more than 10 items
    const pageOneButton = screen.getByRole('button', { name: '1' });
    expect(pageOneButton).toHaveAttribute('aria-current', 'page');
    
    // Click next page
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByText('User 11')).toBeInTheDocument();
  });

  it('handles delete member', async () => {
    const deleteMutate = vi.fn();
    (useMember as any).mockReturnValue({
      members: { data: mockMembers, isLoading: false, error: null },
      searchMembers: () => ({ data: mockMembers, isLoading: false, error: null }),
      deleteMember: { mutate: deleteMutate },
    });

    // Mock window.confirm before rendering
    const confirmSpy = vi.spyOn(window, 'confirm');
    confirmSpy.mockImplementation(() => true);

    render(<MemberList />);
    
    // Find delete button by its aria-label
    const deleteButton = screen.getByRole('button', { name: /delete member john doe/i });
    fireEvent.click(deleteButton);

    expect(confirmSpy).toHaveBeenCalledWith('Are you sure you want to delete this member?');
    expect(deleteMutate).toHaveBeenCalledWith(1);
    
    // Clean up
    confirmSpy.mockRestore();
  });
}); 