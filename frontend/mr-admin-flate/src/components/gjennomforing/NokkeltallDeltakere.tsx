import { Heading } from "@navikt/ds-react";
import * as Highcharts from "highcharts";
import HighchartsReact from "highcharts-react-official";
import "highcharts/modules/accessibility";
import { useRef } from "react";
import { useSuspenseGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";

interface Props {
  gjennomforingId: string;
}

export function NokkeltallDeltakere({ gjennomforingId }: Props) {
  const { data: deltakerSummary } = useSuspenseGjennomforingDeltakerSummary(gjennomforingId);
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
    <div className="bg-bg-subtle p-[0.5rem] rounded-[0.5rem] mt-8">
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
          <HighchartsReact highcharts={Highcharts} options={options} ref={chartComponentRef} />
        </>
      ) : null}
    </div>
  );
}
