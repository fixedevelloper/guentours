"use client";

import { useParams } from "next/navigation";
import { useTranslations, useLocale } from "next-intl";
import { toast } from "sonner";

import { useRouter } from "@/i18n/navigation";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { PaymentForm, type PaymentFormValues } from "@/components/checkout/payment-form";
import { useBookingQuery } from "@/hooks/use-booking";
import { usePaymentMutation } from "@/hooks/use-payment";
import { normalizeApiError } from "@/lib/api/client";
import { formatDate, formatMoney } from "@/lib/format";

export default function PaymentPage() {
  const params = useParams<{ bookingId: string }>();
  const bookingId = params.bookingId;
  const t = useTranslations("Payment");
  const locale = useLocale();
  const router = useRouter();

  const bookingQuery = useBookingQuery(bookingId);
  const paymentMutation = usePaymentMutation();

  function handleSubmit(values: PaymentFormValues) {
    paymentMutation.mutate(
      { bookingId, ...values },
      {
        onSuccess: (payment) => {
          if (payment.status === "SUCCEEDED") {
            toast.success(t("success"));
            router.push(`/bookings/${bookingId}`);
          } else {
            toast.error(t("failure", { reason: payment.failureReason ?? "" }));
          }
        },
        onError: (error) => {
          toast.error(normalizeApiError(error).message);
        },
      }
    );
  }

  if (bookingQuery.isLoading) {
    return (
      <div className="mx-auto max-w-md px-4 py-8">
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  const booking = bookingQuery.data;

  if (!booking) {
    return (
      <div className="mx-auto max-w-md px-4 py-8">
        <Alert variant="destructive">
          <AlertDescription>{t("failure", { reason: "" })}</AlertDescription>
        </Alert>
      </div>
    );
  }

  if (booking.status !== "PENDING_PAYMENT" && booking.status !== "DEPOSIT_PAID") {
    router.replace(`/bookings/${bookingId}`);
    return null;
  }

  const isDepositPayment = booking.status === "PENDING_PAYMENT" && booking.paymentPlan === "PAY_LATER";
  const isBalancePayment = booking.status === "DEPOSIT_PAID";

  return (
    <div className="mx-auto max-w-md px-4 py-8">
      <Card>
        <CardHeader>
          <CardTitle>{t("title")}</CardTitle>
          <p className="text-sm text-muted-foreground">
            {isDepositPayment ? t("depositDue") : isBalancePayment ? t("balanceDue") : t("amountDue")}:{" "}
            <span className="font-semibold text-foreground">{formatMoney(booking.amountDue, locale)}</span>
          </p>
          {isDepositPayment && (
            <p className="text-xs text-muted-foreground">
              {t("payLaterNotice", {
                total: formatMoney(booking.price, locale),
                deadline: booking.ticketingDeadline ? formatDate(booking.ticketingDeadline, locale) : "",
              })}
            </p>
          )}
          {isBalancePayment && booking.ticketingDeadline && (
            <p className="text-xs text-muted-foreground">
              {t("balanceDeadlineNotice", { deadline: formatDate(booking.ticketingDeadline, locale) })}
            </p>
          )}
        </CardHeader>
        <CardContent>
          <PaymentForm onSubmit={handleSubmit} isSubmitting={paymentMutation.isPending} />
        </CardContent>
      </Card>
    </div>
  );
}
