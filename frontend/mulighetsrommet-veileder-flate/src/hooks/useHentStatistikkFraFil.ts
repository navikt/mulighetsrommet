import { useEffect, useState } from 'react';
import { StatistikkFraCsvFil } from '../api/models';
import useSisteStatistikkFil from '../api/queries/useSisteStatistikkFil';

export default function useHentStatistikkFraFil(): StatistikkFraCsvFil[] {
  const [text, setText] = useState<string>();
  const [array, setArray] = useState<any[]>([]);
  const { data: statistikkFil } = useSisteStatistikkFil();

  useEffect(() => {
    const load = async (url: string) => {
      const response = await fetch(url);
      const data = await response.text();
      setText(data);
    };

    if (statistikkFil?.statistikkFilUrl) {
      load(statistikkFil?.statistikkFilUrl);
    }
  }, [statistikkFil]);

  const csvFileToArray = (string: string) => {
    const csvHeader = string.slice(0, string.indexOf('\n')).split(';');
    const csvRows = string.slice(string.indexOf('\n') + 1).split('\n');

    const array = csvRows.map(i => {
      const values = i.split(';');
      return csvHeader.reduce((object: any, header, index) => {
        object[header.trim()] = values[index].replace(/\r/g, '').trim();
        return object;
      }, {});
    });
    setArray(array);
  };

  useEffect(() => {
    if (text) {
      csvFileToArray(text);
    }
  }, [text]);

  return array;
}
