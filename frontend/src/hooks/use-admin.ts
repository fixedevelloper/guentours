import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import * as adminApi from "@/lib/api/admin";
import type { PartnerStatus } from "@/lib/api/types";

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

export function useAdminPartnersQuery(status: PartnerStatus | "ALL", page: number) {
  return useQuery({
    queryKey: ["admin-partners", status, page],
    queryFn: () => adminApi.getAdminPartners(status === "ALL" ? undefined : status, page),
  });
}

export function useApprovePartnerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (partnerId: string) => adminApi.approvePartner(partnerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-partners"] });
    },
  });
}

export function useRejectPartnerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (partnerId: string) => adminApi.rejectPartner(partnerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-partners"] });
    },
  });
}