import styles from "./OpprettTilsagn.module.scss";
import { Button, DatePicker, Heading, HGrid, Label, TextField } from "@navikt/ds-react";
import { Metadata } from "@/components/detaljside/Metadata";
import { TilsagnByTiltaksgjennomforingResponse, TilsagnRequest } from "@mr/api-client";
import { addYear, formaterDato } from "@/utils/Utils";
import { TilsagnStatus } from "@/pages/tiltaksgjennomforinger/tilsagn/Tilsagnstabell";
import { FormGroup } from "@/components/skjema/FormGroup";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import {
  InferredOpprettTilsagnSchema,
  OpprettTilsagnSchema,
} from "@/components/tilsagn/OpprettTilsagnSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { PadlockLockedFillIcon, PadlockLockedIcon } from "@navikt/aksel-icons";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { useTiltaksgjennomforingById } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { useNavigate } from "react-router-dom";
import { useOpprettTilsagn } from "@/components/tilsagn/useOpprettTilsagn";
import { useEffect } from "react";

export function OpprettTilsagn({
  tilsagn,
}: {
  tilsagn: TilsagnByTiltaksgjennomforingResponse[number];
}) {
  const navigate = useNavigate();
  const mutation = useOpprettTilsagn();
  const form = useForm<InferredOpprettTilsagnSchema>({
    resolver: zodResolver(OpprettTilsagnSchema),
    defaultValues: tilsagn
      ? {
          id: tilsagn.id,
          beregning: {
            ...tilsagn.beregning,
          },
          kostnadssted: tilsagn.kostnadssted.enhetsnummer,
          periode: {
            start: tilsagn.periodeStart,
            slutt: tilsagn.periodeSlutt,
          },
        }
      : {},
  });

  const { data: tiltaksgjennomforing } = useTiltaksgjennomforingById();

  if (!tiltaksgjennomforing) {
    return null;
  }

  useEffect(() => {
    setValue("beregning.antallPlasser", tiltaksgjennomforing.antallPlasser!);
  }, [tiltaksgjennomforing.antallPlasser]);

  const postData: SubmitHandler<InferredOpprettTilsagnSchema> = async (data): Promise<void> => {
    const request: TilsagnRequest = {
      id: data.id || window.crypto.randomUUID(),
      periodeStart: data.periode.start,
      periodeSlutt: data.periode.slutt,
      kostnadssted: data.kostnadssted,
      beregning: data.beregning,
      tiltaksgjennomforingId: tiltaksgjennomforing.id,
    };

    mutation.mutate(request, {
      onSuccess: navigerTilGjennomforing,
    });
  };

  function navigerTilGjennomforing() {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforing!.id}/tilsagn`);
  }
  console.log(tiltaksgjennomforing, tilsagn);

  const { data: kostnadssteder } = useKostnadssted(
    tiltaksgjennomforing.navRegion?.enhetsnummer
      ? [tiltaksgjennomforing.navRegion.enhetsnummer]
      : [],
  );

  const { handleSubmit, register, setValue, getValues } = form;

  const [from, to] = getValues(["periode.start", "periode.slutt"]);
  const months = new Date(to).getMonth() - new Date(from).getMonth();
  const antallPlasser = getValues("beregning.antallPlasser");

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
          <Metadata header="Tiltaksnavn" verdi={tiltaksgjennomforing.tiltakstype.navn}></Metadata>
          <Metadata header="Arrangør" verdi={tilsagn.arrangor.navn}></Metadata>
          <Metadata header="tiltaksnr" verdi={tiltaksgjennomforing.tiltaksnummer}></Metadata>
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
              <div className={styles.formMetadataLabels}>
                <div>Ordinært</div>
                <div>{tiltaksgjennomforing.tiltaksnummer}</div>
              </div>
            </div>
          </div>
          <div className={styles.formContent}>
            <div className={styles.formContentLeft}>
              <FormProvider {...form}>
                <form onSubmit={handleSubmit(postData)}>
                  <Heading size="small">Periode og plasser</Heading>
                  <div className={styles.formRows}>
                    <DatePicker>
                      <HGrid columns={3} gap={"3"} className={styles.formDateAndStatus}>
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
                            <PadlockLockedFillIcon fontSize="1rem" />
                            Tilsagns status
                          </span>
                          <TilsagnStatus tilsagn={tilsagn} />
                        </div>
                      </HGrid>
                    </DatePicker>
                    <HGrid columns={2} gap={"4"}>
                      <TextField
                        size="small"
                        label="Antall plasser"
                        type="number"
                        {...register("beregning.antallPlasser")}
                      />
                      <TextField
                        size="small"
                        label="Sats per plass per måned"
                        value="TODO"
                        readOnly
                      />
                    </HGrid>
                    <HGrid columns={1}>
                      <FormGroup>
                        <ControlledSokeSelect
                          placeholder="Velg kostnadssted"
                          size="small"
                          label="Kostnadssted"
                          {...register("kostnadssted")}
                          options={
                            kostnadssteder
                              ?.sort((a, b) => a.navn.localeCompare(b.navn))
                              .map(({ navn, enhetsnummer }) => {
                                return {
                                  value: enhetsnummer,
                                  label: `${navn} - ${enhetsnummer}`,
                                };
                              }) ?? []
                          }
                        />
                      </FormGroup>
                    </HGrid>
                  </div>
                </form>
              </FormProvider>
            </div>
            <div className={styles.formContentRight}>
              <Heading size="small">Beløp</Heading>
              <Heading size="xsmall" className={styles.beregnetKostnad}>
                Beregnet kostnad
              </Heading>
              <div className={styles.rowSpaceBetween}>
                <div>
                  {antallPlasser} plasser * {months} mnd
                </div>
                <div>1 640 400 kr</div>
              </div>
              <div className={styles.rowSpaceBetween}>
                <Label size="medium">Total beløp</Label>
                <Label size="medium">1 640 400 kr</Label>
              </div>
            </div>
          </div>
        </div>
        <div className={styles.formActions}>
          <Button size="small" type="submit" onClick={handleSubmit(postData)}>
            Send til beslutning
          </Button>
        </div>
      </div>
    </div>
  );
}
