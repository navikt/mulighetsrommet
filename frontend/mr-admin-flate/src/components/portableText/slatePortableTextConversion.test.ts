import { describe, test, expect } from "vitest";
import { slateToPortableText } from "./slateToPortableText";
import { portableTextToSlate } from "./portableTextToSlate";

describe("slate <-> PortableText - konvertering", () => {
  test("basic paragraph", () => {
    const slate = [
      {
        type: "paragraph",
        children: [{ text: "This is editable" }],
      },
    ];
    const pt = [
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            text: "This is editable",
          },
        ],
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate);
  });

  test("multi word link with bold and italic", () => {
    const slate = [
      {
        type: "paragraph",
        children: [
          { text: "This " },
          {
            type: "link",
            url: "vg.no",
            children: [{ text: "is " }],
          },
          {
            type: "link",
            url: "vg.no",
            children: [
              {
                text: "editable ",
                bold: true,
                italic: true,
              },
            ],
          },
          { text: "" },
        ],
      },
    ];
    const pt = [
      {
        _type: "block",
        markDefs: [
          {
            _type: "link",
            _key: "vg.no",
            href: "vg.no",
          },
        ],
        children: [
          {
            _type: "span",
            text: "This ",
          },
          {
            _type: "span",
            text: "is ",
            marks: ["vg.no"],
          },
          {
            _type: "span",
            text: "editable ",
            marks: ["strong", "em", "vg.no"],
          },
          { _type: "span", text: "" },
        ],
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate);
  });

  test("paragraph with link", () => {
    const slate = [
      {
        type: "paragraph",
        children: [
          { text: "This " },
          {
            type: "link",
            url: "vg.no",
            children: [{ text: "is" }],
          },
          { text: " editable " },
        ],
      },
    ];
    const pt = [
      {
        _type: "block",
        markDefs: [
          {
            _type: "link",
            _key: "vg.no",
            href: "vg.no",
          },
        ],
        children: [
          {
            _type: "span",
            text: "This ",
          },
          {
            _type: "span",
            text: "is",
            marks: ["vg.no"],
          },
          {
            _type: "span",
            text: " editable ",
          },
        ],
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate);
  });

  test("bullet list", () => {
    const slate = [
      {
        type: "bulleted-list",
        children: [
          {
            type: "list-item",
            children: [
              {
                text: "This is editable ",
              },
            ],
          },
        ],
      },
    ];
    const pt = [
      {
        _type: "block",
        children: [
          {
            _type: "span",
            text: "This is editable ",
          },
        ],
        listItem: "bullet",
        markDefs: [],
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate);
  });

  test("bullet list 2 items", () => {
    const slate = [
      {
        type: "bulleted-list",
        children: [
          {
            type: "list-item",
            children: [
              { text: "This " },
              {
                text: "bold",
                bold: true,
              },
            ],
          },
          {
            type: "list-item",
            children: [{ text: "2" }],
          },
        ],
      },
    ];
    const pt = [
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            text: "This ",
          },
          {
            _type: "span",
            text: "bold",
            marks: ["strong"],
          },
        ],
        listItem: "bullet",
      },
      {
        _type: "block",
        markDefs: [],
        children: [{ _type: "span", text: "2" }],
        listItem: "bullet",
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate);
  });

  test("heading and bulllet list", () => {
    const slate = [
      {
        type: "heading-one",
        children: [{ text: "This is editable " }],
      },
      {
        type: "bulleted-list",
        children: [
          {
            type: "list-item",
            children: [{ text: "bullet" }],
          },
        ],
      },
    ];
    const pt = [
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            text: "This is editable ",
          },
        ],
        style: "h1",
      },
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            text: "bullet",
          },
        ],
        listItem: "bullet",
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate);
  });

  test("complex example", () => {
    /*
    <h1>This is editable</h1>
    <ul>
      <li><a href="vg.no">adsf</a></li>
      <li><em>fasdf</em></li>
      <li><b>Bold</b></li>
    </ul>
    <p>Ny paragraf med <a href="nrk.no">link</a></p>
    <ul>
      <li>ny liste<em><b>bolditalic</b></em></li>
    </ul>
    */
    const slate = [
      {
        type: "heading-one",
        children: [
          {
            text: "This is editable ",
          },
        ],
      },
      {
        type: "bulleted-list",
        children: [
          {
            type: "list-item",
            children: [
              {
                text: "",
              },
              {
                type: "link",
                url: "https://vg.no",
                children: [
                  {
                    text: "adsf",
                  },
                ],
              },
              {
                text: "",
              },
            ],
          },
          {
            type: "list-item",
            children: [
              {
                text: "fasdf",
                italic: true,
              },
            ],
          },
          {
            type: "list-item",
            children: [
              {
                text: "Bold",
                bold: true,
              },
            ],
          },
        ],
      },
      {
        type: "paragraph",
        children: [
          {
            text: "Ny paragraf med ",
          },
          {
            type: "link",
            url: "https://nrk.no",
            children: [
              {
                text: "link",
              },
            ],
          },
          {
            text: "",
          },
        ],
      },
      {
        type: "bulleted-list",
        children: [
          {
            type: "list-item",
            children: [
              {
                text: "ny liste ",
              },
              {
                text: "bolditalic",
                bold: true,
                italic: true,
              },
              {
                type: "link",
                url: "https://nrk.no",
                children: [
                  {
                    text: "",
                  },
                ],
              },
              {
                text: "",
              },
            ],
          },
        ],
      },
    ];

    const pt = [
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            marks: undefined,
            text: "This is editable ",
          },
        ],
        style: "h1",
      },
      {
        _type: "block",
        markDefs: [
          {
            _type: "link",
            _key: "https://vg.no",
            href: "https://vg.no",
          },
        ],
        children: [
          {
            _type: "span",
            text: "",
          },
          {
            _type: "span",
            text: "adsf",
            marks: ["https://vg.no"],
          },
          {
            _type: "span",
            text: "",
          },
        ],
        listItem: "bullet",
      },
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            text: "fasdf",
            marks: ["em"],
          },
        ],
        listItem: "bullet",
      },
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            text: "Bold",
            marks: ["strong"],
          },
        ],
        listItem: "bullet",
      },
      {
        _type: "block",
        markDefs: [
          {
            _type: "link",
            _key: "https://nrk.no",
            href: "https://nrk.no",
          },
        ],
        children: [
          {
            _type: "span",
            text: "Ny paragraf med ",
          },
          {
            _type: "span",
            text: "link",
            marks: ["https://nrk.no"],
          },
          {
            _type: "span",
            text: "",
          },
        ],
      },
      {
        _type: "block",
        markDefs: [
          {
            _type: "link",
            _key: "https://nrk.no",
            href: "https://nrk.no",
          },
        ],
        children: [
          {
            _type: "span",
            text: "ny liste ",
          },
          {
            _type: "span",
            text: "bolditalic",
            marks: ["strong", "em"],
          },
          {
            _type: "span",
            text: "",
            marks: ["https://nrk.no"],
          },
          {
            _type: "span",
            text: "",
          },
        ],
        listItem: "bullet",
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate);
  });

  test("list-item is converted to paragraph", () => {
    const slate = [
      {
        type: "list-item",
        children: [{ text: "This is editable" }],
      },
    ];
    const slate_paragraph = [
      {
        type: "paragraph",
        children: [{ text: "This is editable" }],
      },
    ];
    const pt = [
      {
        _type: "block",
        markDefs: [],
        children: [
          {
            _type: "span",
            text: "This is editable",
          },
        ],
      },
    ];

    expect(slateToPortableText(slate)).toEqual(pt);
    expect(portableTextToSlate(pt)).toEqual(slate_paragraph);
  });
});
