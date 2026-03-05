import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { Box, Heading, HStack, VStack } from "@navikt/ds-react";

export function Søknadsinfo({
  journalpostId,
  soknadstidspunkt,
  tilskudd = [],
  belopInnenforMaksgrense,
  maksbelopBegrunnelse,
}: {
  journalpostId?: string;
  soknadstidspunkt?: Date;
  tilskudd?: Array<{
    tilskuddstype?: string;
    nodvendigForOpplaring?: boolean;
    begrunnelse?: string;
  }>;
  belopInnenforMaksgrense?: boolean;
  maksbelopBegrunnelse?: string;
}) {
  const deltakerInfo = "Navn Navnesen / F.nr: XXXXXXXXXXXX";
  return (
    <VStack gap="space-16" align="start">
      <MetadataVStack label="Deltakerinformasjon" value={deltakerInfo} />
      <MetadataVStack label="JournalpostID" value={journalpostId || "-"} />
      <MetadataVStack
        label="Søknadstidspunkt"
        value={soknadstidspunkt ? formaterDato(soknadstidspunkt) : "-"}
      />
      {maksbelopBegrunnelse && (
        <>
          <Heading size="small" level="4">
            Vilkårsvurdering
          </Heading>
          {tilskudd.map((tilskuddItem, index) => (
            <Box
              key={index}
              asChild
              padding="space-16"
              borderColor="neutral"
              borderWidth="1"
              borderRadius="4"
            >
              <HStack gap="space-24" align="start">
                <MetadataVStack
                  label="Tilskudd"
                  value={tilskuddItem.tilskuddstype || "Ikke valgt"}
                />
                <MetadataVStack
                  label="Nødvendig for opplæring"
                  value={tilskuddItem.nodvendigForOpplaring ? "Ja" : "Nei"}
                />
                <MetadataVStack label="Begrunnelse" value={tilskuddItem.begrunnelse || "-"} />
              </HStack>
            </Box>
          ))}
          <Box
            asChild
            padding="space-16"
            width="100%"
            borderColor="neutral"
            borderRadius="4"
            borderWidth="1"
          >
            <HStack gap="space-24" align="start">
              <MetadataVStack
                label="Utbetaling innenfor maksbeløp?"
                value={belopInnenforMaksgrense ? "Ja" : "Nei"}
              />
              <MetadataVStack label="Begrunnelse" value={maksbelopBegrunnelse || "-"} />
            </HStack>
          </Box>
        </>
      )}
    </VStack>
  );
}
