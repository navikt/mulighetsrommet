import styles from "./OpprettTilsagn.module.scss";
import { Button, DatePicker, Heading, HGrid, TextField } from "@navikt/ds-react";
import { Metadata } from "@/components/detaljside/Metadata";
import { TilsagnByTiltaksgjennomforingResponse } from "@mr/api-client";
import { addYear, formaterDato } from "@/utils/Utils";
import { TilsagnStatus } from "@/pages/tiltaksgjennomforinger/tilsagn/Tilsagnstabell";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { FormProvider, useForm } from "react-hook-form";
import {
  InferredOpprettTilsagnSchema,
  OpprettTilsagnSchema,
} from "@/components/tilsagn/OpprettTilsagnSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { PadlockLockedFillIcon, PadlockLockedIcon } from "@navikt/aksel-icons";

export function OpprettTilsagn({
  tilsagn,
}: {
  tilsagn: TilsagnByTiltaksgjennomforingResponse[number];
}) {
  const form = useForm<InferredOpprettTilsagnSchema>({
    resolver: zodResolver(OpprettTilsagnSchema),
    defaultValues: tilsagn
      ? {
          id: tilsagn.id,
          beregning: tilsagn.beregning,
          kostnadssted: tilsagn.kostnadssted.enhetsnummer,
          periode: {
            start: tilsagn.periodeStart,
            slutt: tilsagn.periodeSlutt,
          },
        }
      : {},
  });

  const { handleSubmit, register, setValue } = form;

  console.log(tilsagn);

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.headerActions}>
          <Button variant="tertiary" size="small">
            Avbryt
          </Button>
          <Button variant="secondary" size="small">
            Lagre utkast
          </Button>
        </div>
        <Heading size="large">Opprett tilsagn</Heading>
      </div>
      <div className={styles.content}>
        <Heading size="medium">Tiltaksgjennomføring</Heading>
        <div className={styles.metadatas}>
          <Metadata header="Tiltaksnavn" verdi="TODO"></Metadata>
          <Metadata header="Arrangør" verdi={tilsagn.arrangor.navn}></Metadata>
          <Metadata header="tiltaksnr" verdi="TODO"></Metadata>
          <Metadata header="Startdato" verdi={formaterDato(tilsagn.periodeStart)}></Metadata>
          <Metadata header="Sluttdato" verdi={formaterDato(tilsagn.periodeSlutt)}></Metadata>
          <Metadata header="Sluttdato" verdi={formaterDato(tilsagn.periodeSlutt)}></Metadata>
          <Metadata
            header="Antall plasser"
            verdi={tilsagn.tiltaksgjennomforing.antallPlasser}
          ></Metadata>
          <Metadata
            header="Gjennomførings status"
            verdi={<TilsagnStatus tilsagn={tilsagn} />}
          ></Metadata>
        </div>
        <div className={styles.form}>
          <div className={styles.formHeader}>
            <Heading size="medium">Tilsagn</Heading>
            <div className={styles.formMetadata}>
              <div className={styles.formMetadataLabels}>
                <div>Tilsagnstype:</div>
                <div>Tilsagnsnummer:</div>
              </div>
              <div>
                <div>Ordinært</div>
              </div>
            </div>
          </div>
          <div className={styles.formContent}>
            <div className={styles.formContentLeft}>
              <FormProvider {...form}>
                <Heading size="small">Periode og plasser</Heading>
                <DatePicker>
                  <HGrid columns={3} className={styles.formDateAndStatus}>
                    <ControlledDateInput
                      label="Dato fra"
                      fromDate={new Date(tilsagn.periodeStart)}
                      toDate={addYear(new Date(), 50)}
                      format="iso-string"
                      {...register("periode.start")}
                      size="small"
                    />
                    <ControlledDateInput
                      label="Dato til"
                      fromDate={new Date(tilsagn.periodeSlutt)}
                      toDate={addYear(new Date(), 50)}
                      format="iso-string"
                      {...register("periode.slutt")}
                      size="small"
                    />
                    <div>
                      <span className={styles.formTilsagnsStatus}>
                        <PadlockLockedFillIcon fontSize="1.5rem" />
                        Tilsagns status
                      </span>
                      <TilsagnStatus tilsagn={tilsagn} />
                    </div>
                  </HGrid>
                </DatePicker>
                <HGrid columns={2}>
                  <TextField size="small" label="Antall plasser" type="number" />
                </HGrid>
              </FormProvider>
            </div>
            <div className={styles.formContentRight}>
              <Heading size="small">Beløp</Heading>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
