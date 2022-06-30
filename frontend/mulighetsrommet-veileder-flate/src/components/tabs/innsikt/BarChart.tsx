import React from 'react';
import { BarStackHorizontal } from '@visx/shape';
import { SeriesPoint } from '@visx/shape/lib/types';
import { Group } from '@visx/group';
import { AxisBottom, AxisLeft } from '@visx/axis';
import { Grid } from '@visx/grid';
import { scaleBand, scaleLinear, scaleOrdinal } from '@visx/scale';
import { timeParse, timeFormat } from 'd3-time-format';
import { withTooltip, Tooltip, defaultStyles } from '@visx/tooltip';
import { WithTooltipProvidedProps } from '@visx/tooltip/lib/enhancers/withTooltip';
import { LegendOrdinal } from '@visx/legend';
import datapunkt, {Datapunkt} from "./Datapunkt";

type Status = 'Arbeidstaker m. ytelse/oppf' | 'Kun arbeidstaker' | 'Registrert hos Nav' | 'Ukjent';

function isOfType(value: any): value is Status {
  return ['Arbeidstaker m. ytelse/oppf', 'Kun arbeidstaker' , 'Registrert hos Nav' , 'Ukjent'].includes(value);
}


type TooltipData = {
  bar: SeriesPoint<Datapunkt>;
  key: Status;
  index: number;
  height: number;
  width: number;
  x: number;
  y: number;
  color: string;
};

export type BarStackHorizontalProps = {
  width: number;
  height: number;
  margin?: { top: number; right: number; bottom: number; left: number };
  events?: boolean;
};

const bla = '#748CB2';
const gronn = '#9CC677';
export const gul = '#EACF5E';
const rod = '#F9AD79';
export const background = '#F1F1F1';
const black = '#000000'
const defaultMargin = { top: 40, left: 50, right: 40, bottom: 100 };

const data = [{ tiltakstype: 'AFT', antallManeder: '3 mnd',
  'Arbeidstaker m. ytelse/oppf': 25, 'Kun arbeidstaker': 25, 'Registrert hos Nav': 25,
  Ukjent: 25 }, { tiltakstype: 'AFT', antallManeder: '6 mnd',
  'Arbeidstaker m. ytelse/oppf': 25, 'Kun arbeidstaker': 25, 'Registrert hos Nav': 25,
  Ukjent: 25 }, { tiltakstype: 'AFT', antallManeder: '12 mnd',
  'Arbeidstaker m. ytelse/oppf': 25, 'Kun arbeidstaker': 25, 'Registrert hos Nav': 25,
  Ukjent: 25 }];
const keys = Object.keys(data[0]).filter((d) => isOfType(d)) as Status[];

const temperatureTotals = data.reduce((allTotals, currentDate) => {
  const totalTemperature = keys.reduce((dailyTotal, k) => {
    dailyTotal += Number(currentDate[k]);
    return dailyTotal;
  }, 0);
  allTotals.push(totalTemperature);
  return allTotals;
}, [] as number[]);

// accessors
const getAntallManeder = (d: Datapunkt) => d.antallManeder;

// scales
const temperatureScale = scaleLinear<number>({
  domain: [0, Math.max(...temperatureTotals)],
  nice: false,
});
const dateScale = scaleBand<string>({
  domain: data.map(getAntallManeder),
  padding: 0.2,
});
const colorScale = scaleOrdinal<Status, string>({
  domain: keys,
  range: [bla, gronn, gul, rod],
});

export default withTooltip<BarStackHorizontalProps, TooltipData>(
  ({
     width,
     height,
     events = false,
     margin = defaultMargin,
   }: BarStackHorizontalProps & WithTooltipProvidedProps<TooltipData>) => {
    // bounds
    const xMax = width - margin.left - margin.right;
    const yMax = height - margin.top - margin.bottom;

    temperatureScale.rangeRound([0, xMax]);
    dateScale.rangeRound([yMax, 0]);

    console.log(temperatureScale.range())

    return width < 10 ? null : (
      <div>
        <svg width={width} height={height}>
          <rect width={width} height={height} fill={background} rx={14} />
          <Grid
            top={margin.top}
            left={margin.left}
            xScale={dateScale}
            yScale={temperatureScale}
            numTicksRows={10}
            numTicksColumns={10}
            width={xMax}
            height={yMax}
            stroke="#8F8F8F"
          />
          <Group top={margin.top} left={margin.left}>
            <BarStackHorizontal<Datapunkt, Status>
              data={data}
              keys={keys}
              height={yMax}
              y={getAntallManeder}
              xScale={temperatureScale}
              yScale={dateScale}
              color={colorScale}
            >
              {(barStacks) =>
                barStacks.map((barStack) =>
                  barStack.bars.map((bar) => (
                    <rect
                      key={`barstack-horizontal-${barStack.index}-${bar.index}`}
                      x={bar.x}
                      y={bar.y}
                      width={bar.width}
                      height={10}
                      fill={bar.color}
                    />
                  )),
                )
              }
            </BarStackHorizontal>
            <AxisLeft
              hideTicks
              scale={dateScale}
              stroke={black}
              tickStroke={black}
              tickLabelProps={() => ({
                fill: black,
                fontSize: 11,
                textAnchor: 'end',
                dy: '0.33em',
              })}
            />
            <AxisBottom
              hideTicks
              top={yMax}
              scale={temperatureScale}
              stroke={black}
              tickStroke={black}
              tickLabelProps={() => ({
                fill: black,
                fontSize: 14,
                textAnchor: 'middle',
              })}
            />
          </Group>
        </svg>
        <div
          style={{
            width: '100%',
            display: 'flex',
            justifyContent: 'center',
            fontSize: '14px',
            marginTop: '-4rem',
          }}
        >
          <LegendOrdinal scale={colorScale} direction="row" labelMargin="0 15px 0 0" />
        </div>

      </div>
    );
  },
);
