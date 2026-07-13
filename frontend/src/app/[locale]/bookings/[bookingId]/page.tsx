"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { CreditCard, Loader2, PlaneLanding, PlaneTakeoff, XCircle } from "lucide-react";
import { toast } from "sonner";

import { useRouter } from "@/i18n/navigation";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { StatusBadge } from "@/components/tracking/status-badge";
import { TicketList } from "@/components/tracking/ticket-list";
import { useBookingQuery, useCancelBookingMutation } from "@/hooks/use-booking";
import { useBookingTracking } from "@/hooks/use-booking-tracking";
import { normalizeApiError } from "@/lib/api/client";
import { airlineLabel, formatDateTime, formatMoney } from "@/lib/format";
import type { BookingStatus } from "@/lib/api/types";

const IN_PROGRESS_STATUSES: BookingStatus[] = ["PENDING_PAYMENT", "PAID", "CONFIRMING"];

export default function BookingTrackingPage() {
  const params = useParams<{ bookingId: string }>();
  const bookingId = params.bookingId;
  const t = useTranslations("Tracking");
  const tCommon = useTranslations("Common");
  const tPayment = useTranslations("Payment");
  const locale = useLocale();
  const router = useRouter();
  const [confirmingCancel, setConfirmingCancel] = useState(false);

  const bookingQuery = useBookingQuery(bookingId);
  const tracking = useBookingTracking(bookingId);
  const cancelMutation = useCancelBookingMutation(bookingId);

  // The REST fetch (always fresh, see useBookingQuery's staleTime) is the source of truth;
  // a live SSE status only overrides it once actually received, since the provider
  // confirmation is often already finished (mock mode is near-instant) by the time this
  // page subscribes, in which case the backend never has anything left to push.
  const status = tracking.liveStatus ?? bookingQuery.data?.status;

  useEffect(() => {
    if (tracking.isTerminal) {
      bookingQuery.refetch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tracking.isTerminal]);

  function handleCancel() {
    cancelMutation.mutate(undefined, {
      onSuccess: () => setConfirmingCancel(false),
      onError: (error) => toast.error(normalizeApiError(error).message),
    });
  }

  if (bookingQuery.isLoading) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  const booking = bookingQuery.data;
  if (!booking || !status) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <Alert variant="destructive">
          <AlertDescription>{t("title")}</AlertDescription>
        </Alert>
      </div>
    );
  }

  const canCancel = status !== "CANCELLED" && status !== "FAILED";
  const inProgress = IN_PROGRESS_STATUSES.includes(status) && !tracking.connectionError;
  const needsPayment = status === "PENDING_PAYMENT" || status === "DEPOSIT_PAID";

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <Card>
        <CardHeader className="flex-row items-center justify-between">
          <div>
            <CardTitle>{t("title")}</CardTitle>
            <p className="text-sm text-muted-foreground">
              {t("reference")}: <span className="font-mono">{booking.id}</span>
            </p>
          </div>
          <StatusBadge status={status} />
        </CardHeader>
        <CardContent className="grid gap-4">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            {inProgress && <Loader2 className="size-4 animate-spin" />}
            {t(`statusDescription.${status}`)}
          </div>

          {status === "FAILED" && booking.failureReason && (
            <Alert variant="destructive">
              <AlertDescription>{t("failureReason", { reason: booking.failureReason })}</AlertDescription>
            </Alert>
          )}

          {booking.itineraryLegs.length > 0 && (
            <>
              <Separator />
              <div className="grid gap-2">
                {booking.itineraryLegs.map((leg) => (
                  <div key={leg.legIndex} className="grid gap-0.5 rounded-lg border border-border/60 p-2 text-sm">
                    <span className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
                      <span className="rounded bg-muted px-1.5 py-0.5 font-semibold tracking-wide text-foreground">
                        {leg.airline}{leg.flightNumber}
                      </span>
                      {airlineLabel(leg.airline)}
                    </span>
                    <span className="flex items-center gap-2">
                      <PlaneTakeoff className="size-3.5 text-muted-foreground" />
                      {leg.origin} — {formatDateTime(leg.departureTime, locale)}
                    </span>
                    <span className="flex items-center gap-2">
                      <PlaneLanding className="size-3.5 text-muted-foreground" />
                      {leg.destination} — {formatDateTime(leg.arrivalTime, locale)}
                    </span>
                  </div>
                ))}
              </div>
            </>
          )}

          <Separator />

          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">{t("totalPrice")}</span>
            <span className="font-semibold">{formatMoney(booking.price, locale)}</span>
          </div>

          {status === "DEPOSIT_PAID" && (
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">{tPayment("balanceDue")}</span>
              <span className="font-semibold">{formatMoney(booking.amountDue, locale)}</span>
            </div>
          )}

          {needsPayment && (
            <Button size="sm" className="justify-self-start" onClick={() => router.push(`/payment/${bookingId}`)}>
              <CreditCard />
              {status === "DEPOSIT_PAID" ? t("payBalanceAction") : t("payNowAction")}
            </Button>
          )}

          {status === "CONFIRMED" && (
            <>
              <Separator />
              <div>
                <h2 className="mb-3 font-medium">{t("viewTickets")}</h2>
                <TicketList bookingId={bookingId} enabled={status === "CONFIRMED"} />
              </div>
            </>
          )}

          {canCancel && (
            <>
              <Separator />
              {confirmingCancel ? (
                <div className="flex items-center gap-2">
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={handleCancel}
                    disabled={cancelMutation.isPending}
                  >
                    {cancelMutation.isPending ? t("cancelling") : t("cancelConfirm")}
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => setConfirmingCancel(false)}>
                    {tCommon("cancel")}
                  </Button>
                </div>
              ) : (
                <Button
                  variant="outline"
                  size="sm"
                  className="justify-self-start text-destructive hover:text-destructive"
                  onClick={() => setConfirmingCancel(true)}
                >
                  <XCircle />
                  {t("cancelAction")}
                </Button>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
