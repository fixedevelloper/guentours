import { useMutation, type UseMutationOptions } from "@tanstack/react-query";

import * as paymentApi from "@/lib/api/payment";
import type { PaymentRequest, PaymentResponse } from "@/lib/api/types"; // Suppose qu'il existe un PaymentResponse

export function usePaymentMutation(
    options?: UseMutationOptions<PaymentResponse, Error, PaymentRequest>
) {
  return useMutation({
    mutationFn: (request: PaymentRequest) => paymentApi.pay(request),
    ...options,
  });
}