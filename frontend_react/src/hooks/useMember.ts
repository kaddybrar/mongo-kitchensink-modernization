import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberService } from '@/services/memberService';
import type { CreateMemberDto, UpdateMemberDto } from '@/types/member';

export const useMember = () => {
  const queryClient = useQueryClient();

  const members = useQuery({
    queryKey: ['members'],
    queryFn: memberService.getAll,
  });

  const memberDetails = (id: number) =>
    useQuery({
      queryKey: ['member', id],
      queryFn: () => memberService.getById(id),
    });

  const createMember = useMutation({
    mutationFn: (member: CreateMemberDto) => memberService.create(member),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['members'] });
    },
  });

  const updateMember = useMutation({
    mutationFn: ({ id, member }: { id: number; member: UpdateMemberDto }) =>
      memberService.update(id, member),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['members'] });
    },
  });

  const deleteMember = useMutation({
    mutationFn: (id: number) => memberService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['members'] });
    },
  });

  const searchMembers = (query: string) =>
    useQuery({
      queryKey: ['members', 'search', query],
      queryFn: () => memberService.search(query),
      enabled: !!query,
    });

  return {
    members,
    memberDetails,
    createMember,
    updateMember,
    deleteMember,
    searchMembers,
  };
}; 