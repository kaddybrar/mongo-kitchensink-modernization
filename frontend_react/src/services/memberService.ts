import { api } from './api';
import type { Member, CreateMemberDto, UpdateMemberDto, MemberResponse, MembersResponse } from '@/types/member';

export const memberService = {
  getAll: async (): Promise<Member[]> => {
    const response = await api.get<MembersResponse>('/members');
    console.log('API Response:', response);
    return Array.isArray(response.data) ? response.data : response.data.data;
  },

  getById: async (id: number): Promise<Member> => {
    const response = await api.get<MemberResponse>(`/members/${id}`);
    console.log('API Response for getById:', response);
    return response.data.data;
  },

  create: async (member: CreateMemberDto): Promise<Member> => {
    const response = await api.post<MemberResponse>('/members', member);
    console.log('API Response for create:', response);
    return response.data.data;
  },

  update: async (id: number, member: UpdateMemberDto): Promise<Member> => {
    const response = await api.put<MemberResponse>(`/members/${id}`, member);
    console.log('API Response for update:', response);
    return response.data.data;
  },

  delete: async (id: number): Promise<void> => {
    const response = await api.delete(`/members/${id}`);
    console.log('API Response for delete:', response);
  },

  search: async (query: string): Promise<Member[]> => {
    const response = await api.get<MembersResponse>('/members/search', {
      params: { name: query },
    });
    console.log('API Response for search:', response);
    return Array.isArray(response.data) ? response.data : response.data.data;
  },
}; 