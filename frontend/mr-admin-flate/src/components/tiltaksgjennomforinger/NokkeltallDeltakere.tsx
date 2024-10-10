import { Toggles } from "@mr/api-client";
import { Heading } from "@navikt/ds-react";
import * as Highcharts from "highcharts";
import HighchartsReact from "highcharts-react-official";
import { useRef } from "react";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { useTiltaksgjennomforingDeltakerSummary } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import styles from "./NokkeltallDeltakere.module.scss";

interface Props {
  tiltaksgjennomforingId: string;
}

export function NokkeltallDeltakere(props: Props) {
  const { data: enableDebug } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_DEBUGGER,
  );

  if (!enableDebug) {
    return null;
  }

  return <NokkeltallDeltakereGraph {...props} />;
}

function NokkeltallDeltakereGraph({ tiltaksgjennomforingId }: Props) {
  const { data: deltakerSummary } = useTiltaksgjennomforingDeltakerSummary(tiltaksgjennomforingId);
  const chartComponentRef = useRef<HighchartsReact.RefObject>(null);

  const dataArray = deltakerSummary.deltakereByStatus.map(({ status, count }) => ({
    name: status,
    y: count,
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
        name: "Deltakere",
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
          {deltakerSummary.antallDeltakere === 1 ? "deltaker" : "deltakere"}
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
