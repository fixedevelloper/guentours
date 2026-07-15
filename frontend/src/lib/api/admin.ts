import { apiClient } from "./client";
import type { AdminUserResponse, BookingResponse, CommissionWalletBalanceResponse } from "./types";

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
