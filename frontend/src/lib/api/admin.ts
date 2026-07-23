import { apiClient } from "./client";
import type {
  AdminUserResponse,
  BookingResponse,
  CommissionWalletBalanceResponse,
  PageResponse,
  PartnerResponse,
  PartnerStatus,
} from "./types";

export async function getAdminBookings() {
  const { data } = await apiClient.get<BookingResponse[]>("/api/admin/bookings");
  return data;
}

export async function getAdminUsers() {
  const { data } = await apiClient.get<AdminUserResponse[]>("/api/admin/users");
  return data;
}

export async function getCommissionWallet() {
  const { data } = await apiClient.get<CommissionWalletBalanceResponse>("/api/admin/commission/wallet");
  return data;
}

export async function getAdminPartners(status: PartnerStatus | undefined, page: number, size = 20) {
  const { data } = await apiClient.get<PageResponse<PartnerResponse>>("/api/partners", {
    params: { status, page, size },
  });
  return data;
}

export async function approvePartner(partnerId: string) {
  const { data } = await apiClient.patch<PartnerResponse>(`/api/partners/${partnerId}/approve`);
  return data;
}

export async function rejectPartner(partnerId: string) {
  const { data } = await apiClient.patch<PartnerResponse>(`/api/partners/${partnerId}/reject`);
  return data;
}