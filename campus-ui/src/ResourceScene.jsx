import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Float, Html, OrbitControls, Sparkles, Stars } from "@react-three/drei";
import * as THREE from "three";

const CAMPUS_POINTS = [
  [-4.6, -1.1], [-2.4, 1.6], [0, -1.8], [2.7, 1.2], [4.8, -0.8],
  [-3.7, 3], [-0.6, 2.8], [2.1, -3], [4.2, 2.7], [-5.2, -3],
];

function ResourceShape({ resource, index, selected, highlighted, onSelect }) {
  const group = useRef();
  const material = useRef();
  const [hovered, setHovered] = useState(false);
  const available = Boolean(resource.available);
  const targetColor = useMemo(() => new THREE.Color(available ? "#55f4b0" : "#ff557e"), [available]);
  const position = CAMPUS_POINTS[index % CAMPUS_POINTS.length];
  const lab = resource.type?.toLowerCase() === "lab";
  const book = resource.type?.toLowerCase() === "book";

  useFrame((state, delta) => {
    if (!group.current || !material.current)
      return;
    const targetScale = hovered ? 1.18 : selected ? 1.08 : 1;
    group.current.scale.lerp(new THREE.Vector3(targetScale, targetScale, targetScale), 1 - Math.exp(-delta * 8));
    material.current.color.lerp(targetColor, 1 - Math.exp(-delta * 3));
    material.current.emissive.lerp(targetColor, 1 - Math.exp(-delta * 3));
    material.current.emissiveIntensity = THREE.MathUtils.lerp(
      material.current.emissiveIntensity,
      hovered || selected ? 1.35 : highlighted ? 0.95 : 0.38,
      1 - Math.exp(-delta * 5),
    );
    if (!selected)
      group.current.rotation.y += delta * 0.12;
    group.current.position.y = Math.sin(state.clock.elapsedTime * 0.8 + index) * 0.12;
  });

  return (
    <Float speed={1.1} rotationIntensity={0.05} floatIntensity={0.12}>
      <group
        ref={group}
        position={[position[0], 0.4, position[1]]}
        onPointerOver={(event) => { event.stopPropagation(); setHovered(true); document.body.style.cursor = "pointer"; }}
        onPointerOut={() => { setHovered(false); document.body.style.cursor = "auto"; }}
        onClick={(event) => { event.stopPropagation(); onSelect(resource, group.current); }}
      >
        {lab ? (
          <mesh castShadow>
            <cylinderGeometry args={[0.62, 0.78, 0.8, 6]} />
            <meshStandardMaterial ref={material} color={targetColor} emissive={targetColor} emissiveIntensity={0.38} metalness={0.55} roughness={0.28} />
          </mesh>
        ) : book ? (
          <mesh castShadow>
            <boxGeometry args={[0.78, 0.92, 0.34]} />
            <meshStandardMaterial ref={material} color={targetColor} emissive={targetColor} emissiveIntensity={0.38} metalness={0.35} roughness={0.32} />
          </mesh>
        ) : (
          <mesh castShadow>
            <octahedronGeometry args={[0.7, 0]} />
            <meshStandardMaterial ref={material} color={targetColor} emissive={targetColor} emissiveIntensity={0.38} metalness={0.55} roughness={0.24} />
          </mesh>
        )}
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.34, 0]}>
          <ringGeometry args={[0.84, 0.89, 48]} />
          <meshBasicMaterial color={targetColor} transparent opacity={0.5} />
        </mesh>
        {(hovered || selected) && (
          <Html position={[0, 1.05, 0]} center distanceFactor={10} style={{ pointerEvents: "none" }}>
            <div className="scene-label">{resource.name}<span>{resource.id}</span></div>
          </Html>
        )}
        {highlighted && <pointLight color={targetColor} intensity={1.4} distance={3.4} />}
      </group>
    </Float>
  );
}

function CampusGround() {
  return (
    <group position={[0, -0.45, 0]}>
      <mesh rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <planeGeometry args={[15, 11]} />
        <meshStandardMaterial color="#0b1220" metalness={0.4} roughness={0.72} />
      </mesh>
      <gridHelper args={[15, 30, "#17434b", "#142735"]} position={[0, 0.015, 0]} />
      <mesh position={[0, 0.05, 0]}>
        <torusGeometry args={[2.3, 0.012, 6, 90]} />
        <meshBasicMaterial color="#37d8dc" transparent opacity={0.26} />
      </mesh>
    </group>
  );
}

function SceneContent({ resources, selectedId, query, searchMatches, onSelect, controlsRef, onInteract, autoRotate }) {
  const camera = useRef();
  const destination = useRef(null);
  const handleSelect = useCallback((resource, object) => {
    const world = new THREE.Vector3();
    object.getWorldPosition(world);
    destination.current = { target: world, position: world.clone().add(new THREE.Vector3(5, 4, 7)) };
    onSelect(resource);
  }, [onSelect]);

  useEffect(() => {
    if (!camera.current || !controlsRef.current)
      return;
    const active = resources.find((resource) => resource.id === selectedId);
    if (!active)
      destination.current = null;
  }, [resources, selectedId, controlsRef]);

  useFrame((_, delta) => {
    if (!destination.current || !camera.current || !controlsRef.current)
      return;
    camera.current.position.lerp(destination.current.position, 1 - Math.exp(-delta * 1.8));
    controlsRef.current.target.lerp(destination.current.target, 1 - Math.exp(-delta * 1.8));
    controlsRef.current.update();
  });

  const normalizedQuery = query.trim().toLowerCase();
  return (
    <>
      <perspectiveCamera ref={camera} makeDefault position={[10, 9, 13]} fov={42} />
      <color attach="background" args={["#080b12"]} />
      <fog attach="fog" args={["#080b12", 13, 28]} />
      <ambientLight intensity={0.72} />
      <directionalLight position={[5, 9, 4]} intensity={1.5} color="#a5e8ff" castShadow />
      <pointLight position={[-5, 4, -4]} intensity={28} distance={14} color="#4b81ff" />
      <Stars radius={90} depth={45} count={1800} factor={3} saturation={0.2} fade speed={0.5} />
      <Sparkles count={50} scale={[13, 4, 10]} size={1.6} speed={0.2} color="#45decb" position={[0, 2, 0]} />
      <CampusGround />
      {resources.map((resource, index) => (
        <ResourceShape
          key={resource.id}
          resource={resource}
          index={index}
          selected={resource.id === selectedId}
          highlighted={Boolean(normalizedQuery) && searchMatches.has(resource.id)}
          onSelect={handleSelect}
        />
      ))}
      <OrbitControls
        ref={controlsRef}
        enableDamping
        dampingFactor={0.08}
        minDistance={7}
        maxDistance={23}
        maxPolarAngle={Math.PI / 2.05}
        autoRotateSpeed={0.42}
        autoRotate={autoRotate}
        onStart={onInteract}
      />
    </>
  );
}

export default function ResourceScene({ resources, selectedId, query, searchMatches, onSelect }) {
  const controlsRef = useRef();
  const [autoRotate, setAutoRotate] = useState(true);
  const onInteract = useCallback(() => {
    setAutoRotate(false);
  }, []);

  return (
    <Canvas shadows dpr={[1, 1.8]} camera={{ position: [10, 9, 13], fov: 42 }}>
      <SceneContent
        resources={resources}
        selectedId={selectedId}
        query={query}
        searchMatches={searchMatches}
        onSelect={onSelect}
        controlsRef={controlsRef}
        onInteract={onInteract}
      />
    </Canvas>
  );
}
