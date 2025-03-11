export interface Member {
  id: number;
  name: string;
  email: string;
  phoneNumber?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMemberDto {
  name: string;
  email: string;
  phoneNumber?: string;
}

export interface UpdateMemberDto {
  name?: string;
  email?: string;
  phoneNumber?: string;
}

export interface MemberResponse {
  data: Member;
  message: string;
}

export interface MembersResponse {
  data: Member[];
  message: string;
} 