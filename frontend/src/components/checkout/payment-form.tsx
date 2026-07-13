"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useTranslations } from "next-intl";
import { CreditCard, Lock, Smartphone } from "lucide-react";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import type { PaymentMethod } from "@/lib/api/types";

const schema = z.object({
  paymentMethod: z.enum(["CARD", "MTN_MOBILE_MONEY", "ORANGE_MONEY"]),
  cardNumber: z.string().trim().optional(),
  cardHolderName: z.string().trim().optional(),
  expiry: z.string().trim().optional(),
  cvv: z.string().trim().optional(),
  mobileNumber: z.string().trim().optional(),
});

export type PaymentFormValues = z.infer<typeof schema>;

interface PaymentFormProps {
  onSubmit: (values: PaymentFormValues) => void;
  isSubmitting: boolean;
}

const METHODS: { value: PaymentMethod; labelKey: "methodCard" | "methodMtn" | "methodOrange" }[] = [
  { value: "CARD", labelKey: "methodCard" },
  { value: "MTN_MOBILE_MONEY", labelKey: "methodMtn" },
  { value: "ORANGE_MONEY", labelKey: "methodOrange" },
];

export function PaymentForm({ onSubmit, isSubmitting }: PaymentFormProps) {
  const t = useTranslations("Payment");

  const form = useForm<PaymentFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      paymentMethod: "CARD",
      cardNumber: "",
      cardHolderName: "",
      expiry: "",
      cvv: "",
      mobileNumber: "",
    },
  });

  const paymentMethod = form.watch("paymentMethod");
  const isMobileMoney = paymentMethod !== "CARD";

  function handleSubmit(values: PaymentFormValues) {
    if (values.paymentMethod === "CARD") {
      if (!values.cardNumber?.match(/^\d{12,19}$/) || !values.cardHolderName?.trim()
          || !values.expiry?.match(/^(0[1-9]|1[0-2])\/\d{2}$/) || !values.cvv?.match(/^\d{3,4}$/)) {
        form.trigger();
        return;
      }
    } else if (!values.mobileNumber?.match(/^\+?\d{8,15}$/)) {
      form.setError("mobileNumber", { message: t("mobileNumberInvalid") });
      return;
    }
    onSubmit(values);
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="grid gap-4">
        <div className="grid grid-cols-3 gap-2">
          {METHODS.map((method) => (
            <button
              key={method.value}
              type="button"
              onClick={() => form.setValue("paymentMethod", method.value)}
              className={cn(
                "flex flex-col items-center gap-1.5 rounded-xl border p-3 text-center transition-colors",
                paymentMethod === method.value ? "border-primary bg-primary/5" : "border-border hover:bg-accent/40"
              )}
            >
              {method.value === "CARD" ? (
                <CreditCard className="size-4" />
              ) : (
                <Smartphone className="size-4" />
              )}
              <span className="text-xs font-medium">{t(method.labelKey)}</span>
            </button>
          ))}
        </div>

        {isMobileMoney ? (
          <FormField
            control={form.control}
            name="mobileNumber"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t("mobileNumber")}</FormLabel>
                <FormControl>
                  <Input inputMode="tel" placeholder="+237670000000" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        ) : (
          <>
            <FormField
              control={form.control}
              name="cardNumber"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("cardNumber")}</FormLabel>
                  <FormControl>
                    <Input inputMode="numeric" placeholder="4242424242421234" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="cardHolderName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("cardHolderName")}</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="expiry"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("expiry")}</FormLabel>
                    <FormControl>
                      <Input placeholder="12/30" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="cvv"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("cvv")}</FormLabel>
                    <FormControl>
                      <Input inputMode="numeric" placeholder="123" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </>
        )}

        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Lock className="size-3.5" />
          {t("secureNotice")}
        </p>

        <Button type="submit" size="lg" disabled={isSubmitting}>
          <CreditCard />
          {isSubmitting ? t("processing") : t("submit")}
        </Button>
      </form>
    </Form>
  );
}
