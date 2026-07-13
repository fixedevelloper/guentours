"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useFieldArray, useForm } from "react-hook-form";
import { useTranslations } from "next-intl";
import { Plus, Trash2 } from "lucide-react";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { CheckoutRequest, PassengerType } from "@/lib/api/types";
import { cn } from "@/lib/utils";

const travelerSchema = z.object({
  fullName: z.string().trim().min(1, ""),
  dateOfBirth: z.string().optional(),
  passportNumber: z.string().optional(),
  type: z.enum(["ADULT", "CHILD", "INFANT"]),
});

const schema = z.object({
  contactEmail: z.string().trim().email(""),
  contactFullName: z.string().trim().min(1, ""),
  contactPhone: z.string().optional(),
  travelers: z.array(travelerSchema).min(1),
  paymentPlan: z.enum(["PAY_NOW", "PAY_LATER"]),
});

export type CheckoutFormValues = z.infer<typeof schema>;

interface CheckoutFormProps {
  depositPercentageLabel?: string;
  onSubmit: (request: Omit<CheckoutRequest, "offerId" | "offerType">) => void;
  isSubmitting: boolean;
}

export function CheckoutForm({ depositPercentageLabel, onSubmit, isSubmitting }: CheckoutFormProps) {
  const t = useTranslations("Checkout");

  const form = useForm<CheckoutFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      contactEmail: "",
      contactFullName: "",
      contactPhone: "",
      travelers: [{ fullName: "", dateOfBirth: "", passportNumber: "", type: "ADULT" }],
      paymentPlan: "PAY_NOW",
    },
  });

  const { fields, append, remove } = useFieldArray({ control: form.control, name: "travelers" });
  const paymentPlan = form.watch("paymentPlan");

  function handleSubmit(values: CheckoutFormValues) {
    onSubmit({
      contactEmail: values.contactEmail,
      contactFullName: values.contactFullName,
      contactPhone: values.contactPhone || undefined,
      travelers: values.travelers.map((traveler) => ({
        fullName: traveler.fullName,
        dateOfBirth: traveler.dateOfBirth || undefined,
        passportNumber: traveler.passportNumber || undefined,
        type: traveler.type,
      })),
      paymentPlan: values.paymentPlan,
    });
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="grid gap-6">
        <Card>
          <CardHeader>
            <CardTitle>{t("contactSection")}</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="contactEmail"
              render={({ field }) => (
                <FormItem className="sm:col-span-2">
                  <FormLabel>{t("contactEmail")}</FormLabel>
                  <FormControl>
                    <Input type="email" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="contactFullName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("contactFullName")}</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="contactPhone"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t("contactPhone")}</FormLabel>
                  <FormControl>
                    <Input type="tel" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("travelersSection")}</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4">
            {fields.map((field, index) => (
              <div key={field.id}>
                {index > 0 && <Separator className="mb-4" />}
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-sm font-medium">{t("traveler", { index: index + 1 })}</span>
                  {fields.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => remove(index)}
                      aria-label={t("traveler", { index: index + 1 })}
                    >
                      <Trash2 className="text-destructive" />
                    </Button>
                  )}
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField
                    control={form.control}
                    name={`travelers.${index}.fullName`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("fullName")}</FormLabel>
                        <FormControl>
                          <Input {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name={`travelers.${index}.type`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("passengerType")}</FormLabel>
                        <Select
                          value={field.value}
                          onValueChange={(v) => field.onChange(v as PassengerType)}
                        >
                          <FormControl>
                            <SelectTrigger className="w-full">
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="ADULT">{t("adult")}</SelectItem>
                            <SelectItem value="CHILD">{t("child")}</SelectItem>
                            <SelectItem value="INFANT">{t("infant")}</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name={`travelers.${index}.dateOfBirth`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("dateOfBirth")}</FormLabel>
                        <FormControl>
                          <Input type="date" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name={`travelers.${index}.passportNumber`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("passportNumber")}</FormLabel>
                        <FormControl>
                          <Input {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
              </div>
            ))}

            <Button
              type="button"
              variant="outline"
              size="sm"
              className="justify-self-start"
              onClick={() => append({ fullName: "", dateOfBirth: "", passportNumber: "", type: "ADULT" })}
            >
              <Plus />
              {t("traveler", { index: fields.length + 1 })}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("paymentPlanSection")}</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2">
            {(["PAY_NOW", "PAY_LATER"] as const).map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => form.setValue("paymentPlan", value)}
                className={cn(
                  "grid gap-1 rounded-xl border p-4 text-left transition-colors",
                  paymentPlan === value ? "border-primary bg-primary/5" : "border-border hover:bg-accent/40"
                )}
              >
                <span className="text-sm font-semibold">
                  {value === "PAY_NOW" ? t("payNow") : t("payLater")}
                </span>
                <span className="text-xs text-muted-foreground">
                  {value === "PAY_NOW"
                    ? t("payNowDescription")
                    : t("payLaterDescription", { percentage: depositPercentageLabel ?? "20" })}
                </span>
              </button>
            ))}
          </CardContent>
        </Card>

        <p className="text-sm text-muted-foreground">{t("guestNotice")}</p>

        <Button type="submit" size="lg" disabled={isSubmitting}>
          {isSubmitting ? t("submitting") : t("submit")}
        </Button>
      </form>
    </Form>
  );
}
