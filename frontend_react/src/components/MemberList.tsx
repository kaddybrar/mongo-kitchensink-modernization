import { useState } from 'react';
import { useMember } from '../hooks/useMember';
import { PencilIcon, TrashIcon } from '@heroicons/react/24/outline';
import { SearchBar } from './SearchBar';
import { Pagination } from './Pagination';
import { MemberForm } from './MemberForm';

const ITEMS_PER_PAGE = 10;

export const MemberList = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const { members, deleteMember, searchMembers } = useMember();

  const searchResults = searchMembers(searchQuery);
  const displayData = searchQuery ? searchResults.data : members.data;
  const isLoading = searchQuery ? searchResults.isLoading : members.isLoading;
  const error = searchQuery ? searchResults.error : members.error;

  // Calculate pagination
  const totalItems = displayData?.length || 0;
  const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const endIndex = startIndex + ITEMS_PER_PAGE;
  const currentItems = displayData?.slice(startIndex, endIndex) || [];

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    setCurrentPage(1); // Reset to first page on new search
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handleAddMember = () => {
    setIsAddModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsAddModalOpen(false);
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="loading-spinner" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-text" role="alert">
        Error loading members: {error.message}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="sm:flex sm:items-center sm:justify-between">
        <div className="w-full max-w-xs">
          <SearchBar onSearch={handleSearch} placeholder="Search members..." />
        </div>
        <div className="mt-4 sm:mt-0">
          <button 
            className="btn-primary"
            onClick={handleAddMember}
          >
            Add Member
          </button>
        </div>
      </div>

      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Email</th>
              <th scope="col">Phone</th>
              <th scope="col" className="relative">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {currentItems.map((member) => (
              <tr key={member.id}>
                <td>{member.name}</td>
                <td>{member.email}</td>
                <td>{member.phoneNumber || '-'}</td>
                <td className="text-right">
                  <button
                    className="text-primary-600 hover:text-primary-900 mr-4"
                    aria-label={`Edit member ${member.name}`}
                  >
                    <PencilIcon className="h-5 w-5" />
                  </button>
                  <button
                    onClick={() => {
                      if (window.confirm('Are you sure you want to delete this member?')) {
                        deleteMember.mutate(member.id);
                      }
                    }}
                    className="text-red-600 hover:text-red-900"
                    aria-label={`Delete member ${member.name}`}
                  >
                    <TrashIcon className="h-5 w-5" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {currentItems.length === 0 && (
          <div className="text-center text-gray-500 py-8">No members found</div>
        )}
      </div>

      {totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={handlePageChange}
        />
      )}

      {isAddModalOpen && (
        <MemberForm onClose={handleCloseModal} />
      )}
    </div>
  );
}; 