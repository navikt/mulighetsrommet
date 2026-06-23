import { Heading } from "@navikt/ds-react";
import { useRef } from "react";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import { Chart, HighchartsReactRefObject, ChartOptions } from "@highcharts/react";
import { Accessibility } from "@highcharts/react/modules/Accessibility";

interface Props {
  gjennomforingId: string;
}

export function NokkeltallDeltakere({ gjennomforingId }: Props) {
  const { data: deltakerSummary } = useGjennomforingDeltakerSummary(gjennomforingId);
  const chartComponentRef = useRef<HighchartsReactRefObject>(null);

  const dataArray = deltakerSummary.deltakereByStatus.map(({ status, count }) => ({
    name: status,
    y: count,
  }));

  const blaafarge = "#66CBEC";
  const oransjeFarge = "#FFC166";
  const options: ChartOptions = {
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
    palette: { colorScheme: "light" },
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
    <div className="bg-ax-bg-neutral-soft p-[0.5rem] rounded-[0.5rem] mt-8">
      <Heading size="small" level="2">
        <span className="font-light pr-4">Deltakerinformasjon</span>{" "}
        <b>
          Totalt {deltakerSummary.antallDeltakere}{" "}
          {deltakerSummary.antallDeltakere === 1 ? "deltaker" : "deltakere"}
        </b>
      </Heading>
      {deltakerSummary.antallDeltakere > 0 ? (
        <>
          <hr />
          <Chart options={options} ref={chartComponentRef}>
            <Accessibility />
          </Chart>
        </>
      ) : null}
    </div>
  );
}
