"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Plane, Building2, Car, Home as HomeIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    CardDescription,
} from "@/components/ui/card";

type PartnerType = "airline" | "hotel" | "car_rental" | "furnished_rental";

const PARTNER_TYPES: { value: PartnerType; label: string; icon: typeof Plane }[] = [
    { value: "airline", label: "Compagnie de voyage / transport", icon: Plane },
    { value: "hotel", label: "Hôtel / établissement d'hébergement", icon: Building2 },
    { value: "car_rental", label: "Agence de location de voiture", icon: Car },
    { value: "furnished_rental", label: "Agence de location de meublé", icon: HomeIcon },
];

export default function BecomeHostPage() {
    const t = useTranslations("BecomeHost");
    const [partnerType, setPartnerType] = useState<PartnerType | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [success, setSuccess] = useState(false);

    const [form, setForm] = useState({
        companyName: "",
        contactName: "",
        email: "",
        phone: "",
        city: "",
        country: "",
        fleetOrRoomsCount: "", // nb avions/hôtels/voitures/logements selon le type
        description: "",
        registrationNumber: "", // RCCM / registre de commerce
    });

    function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
        setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!partnerType) return;
        setSubmitting(true);
        try {
            const res = await fetch("/api/partners/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ partnerType, ...form }),
            });
            if (!res.ok) throw new Error("Échec de l'inscription");
            setSuccess(true);
        } catch (err) {
            console.error(err);
            alert("Une erreur est survenue, veuillez réessayer.");
        } finally {
            setSubmitting(false);
        }
    }

    if (success) {
        return (
            <div className="mx-auto max-w-xl px-4 py-20 text-center">
                <h1 className="text-2xl font-bold">Demande envoyée 🎉</h1>
                <p className="mt-2 text-muted-foreground">
                    Notre équipe va étudier votre dossier et vous recontacter sous 48h.
                </p>
            </div>
        );
    }

    return (
        <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
            <h1 className="text-3xl font-bold">Devenez partenaire</h1>
            <p className="mt-2 text-muted-foreground">
                Rejoignez notre plateforme en tant que compagnie de voyage, hôtel,
                agence de location de voiture ou de meublé.
            </p>

            {/* Étape 1 : choix du type de partenaire */}
            <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {PARTNER_TYPES.map(({ value, label, icon: Icon }) => (
                    <button
                        key={value}
                        type="button"
                        onClick={() => setPartnerType(value)}
                        className={`flex flex-col items-center gap-2 rounded-xl border p-4 text-center text-sm font-medium transition-all ${
                            partnerType === value
                                ? "border-primary bg-primary/10 text-primary"
                                : "border-border/60 text-muted-foreground hover:border-primary/40"
                        }`}
                    >
                        <Icon className="size-6" />
                        {label}
                    </button>
                ))}
            </div>

            {/* Étape 2 : formulaire */}
            {partnerType && (
                <Card className="mt-8">
                    <CardHeader>
                        <CardTitle>Informations de l'entreprise</CardTitle>
                        <CardDescription>
                            Ces informations serviront à valider votre compte partenaire.
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <form onSubmit={handleSubmit} className="space-y-5">
                            <div className="grid gap-4 sm:grid-cols-2">
                                <div>
                                    <Label htmlFor="companyName">Nom de l'entreprise</Label>
                                    <Input
                                        id="companyName"
                                        name="companyName"
                                        required
                                        value={form.companyName}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="registrationNumber">N° RCCM / registre de commerce</Label>
                                    <Input
                                        id="registrationNumber"
                                        name="registrationNumber"
                                        required
                                        value={form.registrationNumber}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="contactName">Nom du responsable</Label>
                                    <Input
                                        id="contactName"
                                        name="contactName"
                                        required
                                        value={form.contactName}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="email">Email professionnel</Label>
                                    <Input
                                        id="email"
                                        name="email"
                                        type="email"
                                        required
                                        value={form.email}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="phone">Téléphone</Label>
                                    <Input
                                        id="phone"
                                        name="phone"
                                        required
                                        value={form.phone}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="city">Ville</Label>
                                    <Input
                                        id="city"
                                        name="city"
                                        required
                                        value={form.city}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="country">Pays</Label>
                                    <Input
                                        id="country"
                                        name="country"
                                        required
                                        value={form.country}
                                        onChange={handleChange}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="fleetOrRoomsCount">
                                        {partnerType === "airline" && "Nombre d'appareils / lignes desservies"}
                                        {partnerType === "hotel" && "Nombre de chambres"}
                                        {partnerType === "car_rental" && "Taille de la flotte (véhicules)"}
                                        {partnerType === "furnished_rental" && "Nombre de logements"}
                                    </Label>
                                    <Input
                                        id="fleetOrRoomsCount"
                                        name="fleetOrRoomsCount"
                                        value={form.fleetOrRoomsCount}
                                        onChange={handleChange}
                                    />
                                </div>
                            </div>

                            <div>
                                <Label htmlFor="description">Description de votre activité</Label>
                                <Textarea
                                    id="description"
                                    name="description"
                                    rows={4}
                                    value={form.description}
                                    onChange={handleChange}
                                />
                            </div>

                            <Button type="submit" disabled={submitting} className="w-full sm:w-auto">
                                {submitting ? "Envoi en cours..." : "Envoyer ma demande"}
                            </Button>
                        </form>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}