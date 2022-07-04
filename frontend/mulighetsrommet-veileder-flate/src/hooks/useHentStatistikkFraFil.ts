import { useEffect, useState } from 'react';

export default function useHentStatistikkFraFil() {
  const [text, setText] = useState<string>();
  const [array, setArray] = useState<any[]>([]);
  useEffect(() => {
    const load = function () {
      fetch('/Statusetteravgang.csv')
        .then(response => response.text())
        .then(responseText => {
          setText(responseText);
        });
    };
    load();
  }, []);

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
