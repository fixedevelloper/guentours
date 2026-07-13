"use client";

import { useLocale, useTranslations } from "next-intl";
import { Printer, Ticket } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTicketsQuery } from "@/hooks/use-tickets";
import { formatDateTime } from "@/lib/format";

export function TicketList({ bookingId, enabled }: { bookingId: string; enabled: boolean }) {
  const t = useTranslations("Tickets");
  const locale = useLocale();
  const { data, isLoading } = useTicketsQuery(bookingId, enabled);

  if (!enabled) return null;

  if (isLoading) {
    return (
      <div className="grid gap-3">
        <Skeleton className="h-20 w-full" />
      </div>
    );
  }

  if (!data || data.length === 0) {
    return <p className="text-sm text-muted-foreground">{t("empty")}</p>;
  }

  return (
    <div className="grid gap-3">
      {data.map((ticket) => (
        <Card key={ticket.id}>
          <CardContent className="flex items-center justify-between gap-4 pt-6">
            <div className="grid gap-1">
              <div className="flex items-center gap-2 font-medium">
                <Ticket className="size-4 text-muted-foreground" />
                {t("ticketNumber", { number: ticket.ticketNumber })}
              </div>
              {ticket.providerConfirmationNumber && (
                <p className="text-sm text-muted-foreground">
                  {t("confirmationNumber", { number: ticket.providerConfirmationNumber })}
                </p>
              )}
              <p className="text-xs text-muted-foreground">
                {t("issuedAt", { date: formatDateTime(ticket.issuedAt, locale) })}
              </p>
            </div>
            <Button variant="outline" size="sm" onClick={() => window.print()}>
              <Printer />
              {t("print")}
            </Button>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
