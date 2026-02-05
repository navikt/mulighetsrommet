import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { GjennomforingDto, PrismodellDto, TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { HelpText, HGrid, HStack, Textarea, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { useFindAvtaltSats } from "@/api/avtaler/useFindAvtaltSats";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  kostnadssteder: KostnadsstedOption[];
}

export function TilsagnFormPrisPerTimeOppfolging(props: Props) {
  return (
    <TilsagnForm
      {...props}
      beregningInput={<BeregningInputSkjema prismodell={props.prismodell} />}
    />
  );
}

function BeregningInputSkjema({ prismodell }: Pick<Props, "prismodell">) {
  const {
    register,
    formState: { errors },
    watch,
    getValues,
  } = useFormContext<TilsagnRequest>();

  const periodeStart = watch("periodeStart");
  const sats = useFindAvtaltSats(prismodell, periodeStart);

  const type = getValues("beregning.type");
  const prisbetingelser = watch("beregning.prisbetingelser");

  return (
    <VStack gap="space-16">
      <MetadataVStack
        label={tilsagnTekster.prismodell.label}
        value={tilsagnTekster.prismodell.sats.label(type)}
      />
      <Textarea
        size="small"
        label={avtaletekster.prisOgBetalingLabel}
        value={prisbetingelser ?? ""}
        readOnly
      />
      <HGrid columns={2}>
        <TextField
          size="small"
          type="number"
          label={tilsagnTekster.antallPlasser.label}
          style={{ width: "180px" }}
          error={errors.beregning?.antallPlasser?.message}
          {...register("beregning.antallPlasser", {
            setValueAs: (v) => (v === "" ? null : Number(v)),
          })}
        />
        <TextField
          size="small"
          type="number"
          label={tilsagnTekster.sats.label(type)}
          style={{ width: "180px" }}
          readOnly
          value={sats?.pris.belop ?? 0}
        />
      </HGrid>
      <HStack gap="space-8" align="start">
        <TextField
          size="small"
          type="number"
          label={tilsagnTekster.antallTimerOppfolgingPerDeltaker.label}
          style={{ width: "180px" }}
          error={errors.beregning?.antallTimerOppfolgingPerDeltaker?.message}
          {...register("beregning.antallTimerOppfolgingPerDeltaker", {
            setValueAs: (v) => (v === "" ? null : Number(v)),
          })}
        />
        <HelpText>
          Antall timer per deltaker til oppfølging, inkludert reisetid og rapportskriving
        </HelpText>
      </HStack>
    </VStack>
  );
}
