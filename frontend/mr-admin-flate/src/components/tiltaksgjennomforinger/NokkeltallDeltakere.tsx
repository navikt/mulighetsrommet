import { Heading } from "@navikt/ds-react";
import * as Highcharts from "highcharts";
import HighchartsReact from "highcharts-react-official";
import { Toggles } from "mulighetsrommet-api-client";
import { useRef } from "react";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { useTiltaksgjennomforingDeltakerSummary } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import styles from "./NokkeltallDeltakere.module.scss";

interface Props {
  tiltaksgjennomforingId: string;
}

export function NokkeltallDeltakere({ tiltaksgjennomforingId }: Props) {
  const { data: enableDebug } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_DEBUGGER,
  );
  const { data: deltakerSummary } = useTiltaksgjennomforingDeltakerSummary(tiltaksgjennomforingId);
  const chartComponentRef = useRef<HighchartsReact.RefObject>(null);

  if (!enableDebug) return null;

  if (!deltakerSummary) return null;

  const summaryUtenTotal = {
    "Påbegynt registrering": deltakerSummary.pabegyntRegistrering,
    "Venter på oppstart": deltakerSummary.antallDeltakereSomVenter,
    Deltar: deltakerSummary.antallAktiveDeltakere,
    "Har sluttet": deltakerSummary.antallAvsluttedeDeltakere,
    "Ikke aktuelle": deltakerSummary.antallIkkeAktuelleDeltakere,
  };

  const dataArray = Object.keys(summaryUtenTotal).map((key) => ({
    name: key,
    y: summaryUtenTotal[key as keyof typeof summaryUtenTotal],
  }));

  const blaafarge = "#66CBEC";
  const oransjeFarge = "#FFC166";
  const options: Highcharts.Options = {
    chart: {
      type: "bar",
      height: 250,
    },
    credits: {
      enabled: false, // Skru av Hightcharts-watermark
    },
    title: {
      text: "Oversikt over deltakere",
      style: {
        display: "none",
      },
    },
    xAxis: {
      categories: dataArray.map((d) => d.name),
      tickInterval: 1,
    },
    yAxis: {
      title: {
        text: "Antall deltakere",
      },
      tickInterval: 1,
    },
    series: [
      {
        type: "bar",
        data: dataArray,
        showInLegend: false,
      },
    ],
    plotOptions: {
      bar: {
        colorByPoint: true,
        colors: [blaafarge, blaafarge, blaafarge, blaafarge, oransjeFarge],
        pointWidth: 20,
        dataLabels: {
          enabled: true,
          inside: true,
          align: "right",
          color: "black",
          style: {
            textOutline: "none",
          },
        },
      },
    },
  };

  return (
    <div className={styles.container}>
      <Heading size="small" level="2">
        <span className={styles.thin}>Deltakerinformasjon</span>{" "}
        <b>
          Totalt {deltakerSummary.antallDeltakere}{" "}
          {deltakerSummary.antallDeltakere > 1 ? "deltakere" : "deltaker"}
        </b>
      </Heading>
      {deltakerSummary.antallDeltakere > 0 ? (
        <>
          <hr />
          <HighchartsReact highcharts={Highcharts} options={options} ref={chartComponentRef} />
        </>
      ) : null}
    </div>
  );
}
