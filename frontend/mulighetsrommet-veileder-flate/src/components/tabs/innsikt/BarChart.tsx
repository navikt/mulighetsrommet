import React from 'react';
import { BarStackHorizontal } from '@visx/shape';
import { SeriesPoint } from '@visx/shape/lib/types';
import { Group } from '@visx/group';
import { AxisBottom, AxisLeft } from '@visx/axis';
import { GridColumns } from '@visx/grid';
import { scaleBand, scaleLinear, scaleOrdinal } from '@visx/scale';
import { LegendOrdinal } from '@visx/legend';
import { Datapunkt } from './Datapunkt';

type Status = 'Arbeidstaker m. ytelse/oppf' | 'Kun arbeidstaker' | 'Registrert hos Nav' | 'Ukjent';

function isOfType(value: any): value is Status {
  return ['Arbeidstaker m. ytelse/oppf', 'Kun arbeidstaker', 'Registrert hos Nav', 'Ukjent'].includes(value);
}

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
const black = '#000000';
const defaultMargin = { top: 20, left: 50, right: 40, bottom: 100 };

const data = [
  {
    tiltakstype: 'AFT',
    antallManeder: '12 mnd',
    'Arbeidstaker m. ytelse/oppf': 25,
    'Kun arbeidstaker': 30,
    'Registrert hos Nav': 30,
    Ukjent: 15,
  },
  {
    tiltakstype: 'AFT',
    antallManeder: '6 mnd',
    'Arbeidstaker m. ytelse/oppf': 30,
    'Kun arbeidstaker': 35,
    'Registrert hos Nav': 20,
    Ukjent: 15,
  },
  {
    tiltakstype: 'AFT',
    antallManeder: '3 mnd',
    'Arbeidstaker m. ytelse/oppf': 35,
    'Kun arbeidstaker': 25,
    'Registrert hos Nav': 15,
    Ukjent: 25,
  },
];
const keys = Object.keys(data[0]).filter(d => isOfType(d)) as Status[];

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
const percentageScale = scaleLinear<number>({
  domain: [0, Math.max(...temperatureTotals)],
  nice: false,
});
const monthScale = scaleBand<string>({
  domain: data.map(getAntallManeder),
  padding: 0.8,
});
const colorScale = scaleOrdinal<Status, string>({
  domain: keys,
  range: [bla, gronn, gul, rod],
});

export default function BarChart({ width, height, margin = defaultMargin }: BarStackHorizontalProps) {
  // bounds
  const xMax = width - margin.left - margin.right;
  const yMax = height - margin.top - margin.bottom;

  percentageScale.rangeRound([0, xMax]);
  monthScale.rangeRound([yMax, 0]);

  return width < 10 ? null : (
    <div>
      <text style={{ fontSize: '16px', fontWeight: 'bold' }}>Status etter avgang</text>
      <svg width={width} height={height}>
        <rect width={width} height={height} fill={background} rx={14} />
        <GridColumns
          top={margin.top}
          left={margin.left}
          scale={percentageScale}
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
            xScale={percentageScale}
            yScale={monthScale}
            color={colorScale}
          >
            {barStacks =>
              barStacks.map(barStack =>
                barStack.bars.map(bar => (
                  <rect
                    key={`barstack-horizontal-${barStack.index}-${bar.index}`}
                    x={bar.x}
                    y={bar.y}
                    width={bar.width}
                    height={bar.height}
                    fill={bar.color}
                  />
                ))
              )
            }
          </BarStackHorizontal>
          <AxisLeft
            hideTicks
            scale={monthScale}
            stroke={black}
            tickStroke={black}
            tickLabelProps={() => ({
              fill: black,
              fontSize: 14,
              textAnchor: 'end',
              dy: '0.25em',
            })}
          />
          <AxisBottom
            hideTicks
            top={yMax}
            scale={percentageScale}
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
        <LegendOrdinal scale={colorScale} direction="row" labelMargin="0 15px 0 0" shapeHeight="8px" shapeWidth="8px" />
      </div>
    </div>
  );
}
