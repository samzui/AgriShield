import { useEffect, useRef } from 'react';
import { Map, Marker, AttributionControl } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

// Geographic center of India — default view before GPS is captured
const INDIA_CENTER: [number, number] = [78.9629, 20.5937];
const INDIA_ZOOM = 4.5;

const OSM_STYLE = {
  version: 8 as const,
  sources: {
    osm: {
      type: 'raster' as const,
      tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
      tileSize: 256,
      attribution: '© OpenStreetMap contributors',
    },
  },
  layers: [{ id: 'osm-tiles', type: 'raster' as const, source: 'osm' }],
};

interface LossMapProps {
  /** Real latitude — null/undefined shows India overview */
  lat?: number | null;
  /** Real longitude — null/undefined shows India overview */
  lng?: number | null;
  height?: number;
}

export default function LossMap({ lat, lng, height = 220 }: LossMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const markerRef = useRef<Marker | null>(null);

  // Create the map once when the container mounts.
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const map = new Map({
      container: el,
      style: OSM_STYLE as any,
      center: INDIA_CENTER,
      zoom: INDIA_ZOOM,
      attributionControl: false,
    });
    map.addControl(new AttributionControl({ compact: true }), 'bottom-right');
    // Resize once tiles have loaded to fix any 0-px canvas edge case.
    map.once('load', () => map.resize());
    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, []); // intentionally empty — map lifecycle is tied to container mount

  // When coordinates change, move the map and update the marker.
  // Runs after init because mapRef is set synchronously inside the init effect.
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    // Remove previous marker if any
    markerRef.current?.remove();
    markerRef.current = null;

    const hasCoords = lat != null && lng != null && isFinite(lat) && isFinite(lng);

    if (hasCoords) {
      markerRef.current = new Marker({ color: '#1f6e45' })
        .setLngLat([lng!, lat!])
        .addTo(map);

      const doFly = () => map.flyTo({ center: [lng!, lat!], zoom: 13, duration: 900 });
      // flyTo is safe to call before 'load' fires in MapLibre,
      // but call it after load to guarantee the style is ready.
      if (map.loaded()) {
        doFly();
      } else {
        map.once('load', doFly);
      }
    } else {
      // GPS cleared or not yet captured — return to India overview
      const doReturn = () => map.flyTo({ center: INDIA_CENTER, zoom: INDIA_ZOOM, duration: 600 });
      if (map.loaded()) {
        doReturn();
      } else {
        map.once('load', doReturn);
      }
    }
  }, [lat, lng]);

  return (
    <div
      ref={containerRef}
      style={{ position: 'relative', height, width: '100%' }}
    />
  );
}
