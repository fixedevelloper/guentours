import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { NextIntlClientProvider } from "next-intl";
import { getMessages, getTranslations, setRequestLocale } from "next-intl/server";
import { Geist, Geist_Mono } from "next/font/google";

import { routing } from "@/i18n/routing";
import { Providers } from "@/components/providers";
import { SiteHeader } from "@/components/site-header";
import { Toaster } from "@/components/ui/sonner";

import "../globals.css";

// Configuration des polices avec variables CSS
const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
  display: "swap", // Assure un chargement fluide sans blocage visuel
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
  display: "swap",
});

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "Metadata" });
  return {
    title: t("title"),
    description: t("description"),
    metadataBase: new URL(process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000"),
  };
}

export default async function LocaleLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;

  // Validation stricte de la locale
  if (!routing.locales.includes(locale as (typeof routing.locales)[number])) {
    notFound();
  }

  // Activation du rendu statique pour cette locale
  setRequestLocale(locale);
  const messages = await getMessages();

  return (
    <html 
      lang={locale} 
      className={`${geistSans.variable} ${geistMono.variable} h-full scroll-smooth antialiased`} 
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col bg-background font-sans text-foreground transition-colors duration-300">
        <NextIntlClientProvider messages={messages}>
          <Providers 
            attribute="class" 
            defaultTheme="system" 
            enableSystem 
            disableTransitionOnChange
          >
            {/* 
              Le Header gère lui-même sa barre du haut collante (sticky) 
              et sa barre du bas fixe (fixed) sans conteneur parent restrictif.
            */}
            <SiteHeader />

            {/* 
              Conteneur de page principal.
              pb-16 sur mobile permet de créer un espace vide en bas de l'écran 
              pour que la barre de navigation fixe ne cache pas le contenu (boutons de validation, etc.).
            */}
            <main className="flex-1 flex flex-col w-full animate-fade-in pb-16 sm:pb-0">
              {children}
            </main>

            {/* Toaster pour les notifications */}
            <Toaster 
              richColors 
              position="top-right" 
              closeButton
              theme="system"
            />
          </Providers>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}