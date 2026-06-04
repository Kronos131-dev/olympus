import type { SVGProps } from "react";

// Icônes fines à trait unique, inspirées de motifs classiques. Couleur = currentColor.
type P = SVGProps<SVGSVGElement> & { size?: number };

function Svg({ size = 24, children, ...rest }: P & { children: React.ReactNode }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.4}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...rest}
    >
      {children}
    </svg>
  );
}

// Temple (accueil)
export const IconHome = (p: P) => (
  <Svg {...p}>
    <path d="M3 9 12 4l9 5" />
    <path d="M5 9v9M9 9v9M15 9v9M19 9v9" />
    <path d="M3 18h18M3 21h18" />
  </Svg>
);

// Laurier / oracle (chat)
export const IconOracle = (p: P) => (
  <Svg {...p}>
    <path d="M12 3c-3 2-4.5 5-4.5 9 0 2 .8 4 2.5 5" />
    <path d="M12 3c3 2 4.5 5 4.5 9 0 2-.8 4-2.5 5" />
    <path d="M12 21v-4" />
  </Svg>
);

// Assiette + couverts (repas)
export const IconMeals = (p: P) => (
  <Svg {...p}>
    <circle cx="11" cy="12" r="7" />
    <circle cx="11" cy="12" r="3.2" />
    <path d="M20 4v16M20 4c-1.2.4-1.8 1.6-1.8 3.2 0 1.4.8 2 1.8 2.2" />
  </Svg>
);

// Parchemin / calendrier (planning)
export const IconPlan = (p: P) => (
  <Svg {...p}>
    <rect x="4" y="5" width="16" height="16" />
    <path d="M4 9h16M8 3v4M16 3v4M8 13h3M8 17h6" />
  </Svg>
);

// Buste / casque (profil)
export const IconProfile = (p: P) => (
  <Svg {...p}>
    <circle cx="12" cy="8" r="4" />
    <path d="M5 21c0-3.5 3-6 7-6s7 2.5 7 6" />
  </Svg>
);

export const IconPlus = (p: P) => (
  <Svg {...p}>
    <path d="M12 5v14M5 12h14" />
  </Svg>
);
export const IconTrash = (p: P) => (
  <Svg {...p}>
    <path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13" />
  </Svg>
);
export const IconSearch = (p: P) => (
  <Svg {...p}>
    <circle cx="11" cy="11" r="7" />
    <path d="m21 21-4.3-4.3" />
  </Svg>
);
export const IconCamera = (p: P) => (
  <Svg {...p}>
    <path d="M3 8h3l2-2h8l2 2h3v11H3z" />
    <circle cx="12" cy="13" r="3.5" />
  </Svg>
);
export const IconMic = (p: P) => (
  <Svg {...p}>
    <rect x="9" y="3" width="6" height="11" rx="3" />
    <path d="M5 11a7 7 0 0 0 14 0M12 18v3" />
  </Svg>
);
export const IconBarcode = (p: P) => (
  <Svg {...p}>
    <path d="M4 6v12M7 6v12M10 6v12M14 6v12M17 6v12M20 6v12" />
  </Svg>
);
export const IconSend = (p: P) => (
  <Svg {...p}>
    <path d="M4 12 20 4l-6 16-3-7z" />
  </Svg>
);
export const IconBack = (p: P) => (
  <Svg {...p}>
    <path d="M15 5l-7 7 7 7" />
  </Svg>
);
export const IconEdit = (p: P) => (
  <Svg {...p}>
    <path d="M4 20h4l10-10-4-4L4 16z" />
    <path d="M13.5 6.5l4 4" />
  </Svg>
);
export const IconSparkle = (p: P) => (
  <Svg {...p}>
    <path d="M12 3v6M12 15v6M3 12h6M15 12h6" />
    <path d="M12 9l1.5 1.5M12 9l-1.5 1.5M12 15l1.5-1.5M12 15l-1.5-1.5" />
  </Svg>
);
export const IconClose = (p: P) => (
  <Svg {...p}>
    <path d="M6 6l12 12M18 6 6 18" />
  </Svg>
);
export const IconCheck = (p: P) => (
  <Svg {...p}>
    <path d="M5 12l5 5L19 6" />
  </Svg>
);
export const IconChevron = (p: P) => (
  <Svg {...p}>
    <path d="M9 6l6 6-6 6" />
  </Svg>
);
export const IconActivity = (p: P) => (
  <Svg {...p}>
    <path d="M3 12h4l2 6 4-14 2 8h6" />
  </Svg>
);
