"use client";

import { Suspense, useMemo } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";

import { useRouter } from "@/i18n/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { CheckoutForm } from "@/components/checkout/checkout-form";
import { OfferSummaryCard } from "@/components/checkout/offer-summary-card";
import { useCheckoutMultiCityMutation, useCheckoutMutation } from "@/hooks/use-booking";
import { normalizeApiError } from "@/lib/api/client";
import { parseOfferSummary } from "@/lib/offer-summary";
import type { CheckoutRequest } from "@/lib/api/types";

export default function CheckoutPage() {
  return (
    <Suspense fallback={<div className="mx-auto max-w-4xl px-4 py-8"><Skeleton className="h-64 w-full" /></div>}>
      <CheckoutPageContent />
    </Suspense>
  );
}

function CheckoutPageContent() {
  const t = useTranslations("Checkout");
  const searchParams = useSearchParams();
  const router = useRouter();
  const checkoutMutation = useCheckoutMutation();
  const multiCityCheckoutMutation = useCheckoutMultiCityMutation();

  const offer = useMemo(() => parseOfferSummary(searchParams), [searchParams]);
  const isSubmitting = checkoutMutation.isPending || multiCityCheckoutMutation.isPending;

  function handleSubmit(partial: Omit<CheckoutRequest, "offerId" | "offerType">) {
    if (!offer) return;

    const callbacks = {
      onSuccess: (booking: { id: string }) => {
        router.push(`/payment/${booking.id}`);
      },
      onError: (error: unknown) => {
        toast.error(normalizeApiError(error).message);
      },
    };

    if (offer.offerType === "MULTI_CITY_FLIGHT") {
      multiCityCheckoutMutation.mutate(
        { ...partial, legOfferIds: offer.legs.map((leg) => leg.offerId) },
        callbacks
      );
    } else {
      checkoutMutation.mutate({ ...partial, offerId: offer.offerId, offerType: offer.offerType }, callbacks);
    }
  }

  if (!offer) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <Alert variant="destructive">
          <AlertTitle>{t("priceExpired")}</AlertTitle>
          <AlertDescription>
            <Button variant="outline" size="sm" className="mt-2" onClick={() => router.push("/")}>
              {t("submit")}
            </Button>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div className="mx-auto grid max-w-4xl gap-6 px-4 py-8 sm:grid-cols-[1fr_320px]">
      <div className="order-2 sm:order-1">
        <h1 className="mb-6 text-2xl font-semibold">{t("title")}</h1>
        <CheckoutForm onSubmit={handleSubmit} isSubmitting={isSubmitting} />
      </div>
      <div className="order-1 sm:order-2">
        <div className="sm:sticky sm:top-20">
          <OfferSummaryCard offer={offer} />
        </div>
      </div>
    </div>
  );
}
