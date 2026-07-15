import { useQuery } from "@tanstack/react-query";

import * as adminApi from "@/lib/api/admin";

export function useAdminBookingsQuery() {
  return useQuery({
    queryKey: ["admin-bookings"],
    queryFn: () => adminApi.getAdminBookings(),
  });
}

export function useAdminUsersQuery() {
  return useQuery({
    queryKey: ["admin-users"],
    queryFn: () => adminApi.getAdminUsers(),
  });
}

export function useCommissionWalletQuery() {
  return useQuery({
    queryKey: ["commission-wallet"],
    queryFn: () => adminApi.getCommissionWallet(),
  });
}
