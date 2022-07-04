import { useEffect, useState } from 'react';
import useSisteStatistikkFil from '../api/queries/useSisteStatistikkFil';
import { stat } from 'fs';

export default function useHentStatistikkFraFil() {
  const [text, setText] = useState<string>();
  const [array, setArray] = useState<any[]>([]);
  const { data: statistikkFil } = useSisteStatistikkFil();

  useEffect(() => {
    const load = function (url :string) {
      fetch(url)
        .then(response => {
          return response.text();
        })
        .then(responseText => {
          setText(responseText);
        });
    };
    if (statistikkFil?.statistikkFilUrl) {
      load(statistikkFil?.statistikkFilUrl);
    }
  }, [statistikkFil]);

  const csvFileToArray = (string: string) => {
    const csvHeader = string.slice(0, string.indexOf('\n')).split(',');
    const csvRows = string.slice(string.indexOf('\n') + 1).split('\n');

    const array = csvRows.map(i => {
      const values = i.split(',');
      return csvHeader.reduce((object: any, header, index) => {
        object[header.trim()] = values[index].trim();
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
