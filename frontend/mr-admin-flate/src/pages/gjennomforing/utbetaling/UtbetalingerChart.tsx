import { UtbetalingerByGjennomforingResponse } from "@mr/api-client-v2";
import HighchartsReact from "highcharts-react-official";
import Highcharts from "highcharts";

export function UtbetalingerChart({
  utbetalinger,
}: {
  utbetalinger: UtbetalingerByGjennomforingResponse;
}) {
  const data = utbetalinger.map((u) => u.beregning.belop);
  const categories = utbetalinger.map((u) => {
    const startDate = new Date(u.beregning.periodeStart);
    const startMonth = startDate.toLocaleString("default", { month: "short" });

    return startMonth;
  });
  const options: Highcharts.Options = {
    chart: {
      type: "column",
      inverted: false,
    },
    title: {
      text: "Oversikt over utbetalinger",
    },
    xAxis: {
      categories: categories,
    },
    series: [
      {
        type: "line",
        data: data,
      },
    ],
  };

  return <HighchartsReact highcharts={Highcharts} options={options} />;
}
