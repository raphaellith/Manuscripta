import React, { useState } from 'react';
import { Header } from './components/Header';
import { ClassDashboard } from './components/ClassDashboard';
import { LessonCreator } from './components/LessonCreator';
import { LessonLibrary } from './components/LessonLibrary';
import { AiAssistant } from './components/AiAssistant';
import { ClassroomControl } from './components/ClassroomControl';
import { Settings } from './components/Settings';
import type { View, ContentItem, LessonFolder, Unit } from './types';
import { ThemeSwitcher } from './components/ThemeSwitcher';



const initialContentItems: ContentItem[] = [
  { id: '1', unit: 'Norman Conquest', lessonNumber: 1, lessonTitle: 'The Battle of Hastings', title: 'Introduction to Hastings', type: 'Lesson', subject: 'History - Year 7-9', created: 'Oct 20, 2025', status: 'Deployed', content: '<h1>The Battle of Hastings</h1><p>The Battle of Hastings was fought on <b>14 October 1066</b> between the Norman-French army of William, the Duke of Normandy, and an English army under the Anglo-Saxon King Harold Godwinson, beginning the Norman conquest of England.</p><ul><li>Key Figure: William the Conqueror</li><li>Key Figure: Harold Godwinson</li></ul>' },
  { id: '1a', unit: 'Norman Conquest', lessonNumber: 1, lessonTitle: 'The Battle of Hastings', title: 'Key Figures Worksheet', type: 'Worksheet', subject: 'History - Year 7-9', created: 'Oct 20, 2025', status: 'Draft', content: '<h3>Worksheet: Key Figures of 1066</h3><p><b>Instructions:</b> For each key figure below, research and write down their claim to the English throne and their role in the events of 1066.</p><br/><h4>1. William, Duke of Normandy</h4><p><b>Claim to the throne:</b></p><br/><p><b>Role in 1066:</b></p><br/><br/><h4>2. Harold Godwinson</h4><p><b>Claim to the throne:</b></p><br/><p><b>Role in 1066:</b></p><br/><br/><h4>3. Harald Hardrada</h4><p><b>Claim to the throne:</b></p><br/><p><b>Role in 1066:</b></p><br/><br/><h4>4. Edward the Confessor</h4><p><b>Role before 1066:</b></p><br/><p><b>Connection to the succession crisis:</b></p>' },
  { id: '1b', unit: 'Norman Conquest', lessonNumber: 1, lessonTitle: 'The Battle of Hastings', title: 'Battle Events Quiz', type: 'Quiz', subject: 'History - Year 7-9', created: 'Oct 20, 2025', status: 'Deployed', content: '<h3>Quiz: The Battle of Hastings</h3><p><b>Instructions:</b> Choose the best answer for each question.</p><br/><p><b>1. What was the date of the Battle of Hastings?</b></p><ul><li>A) 25th December 1066</li><li>B) 14th October 1066</li><li>C) 4th July 1066</li></ul><br/><p><b>2. Where did the battle take place?</b></p><ul><li>A) In London</li><li>B) Near the modern town of Battle, East Sussex</li><li>C) In Normandy, France</li></ul><br/><p><b>3. How was King Harold Godwinson famously depicted to have died in the Bayeux Tapestry?</b></p><ul><li>A) From a sword wound</li><li>B) Falling from his horse</li><li>C) With an arrow to the eye</li></ul>' },
  { id: '4', unit: 'Norman Conquest', lessonNumber: 2, lessonTitle: 'The Domesday Book', title: 'Understanding the Domesday Book', type: 'Lesson', subject: 'History - Year 7-9', created: 'Oct 21, 2025', status: 'Draft', content: '<h1>The Domesday Book</h1><p>The Domesday Book is a manuscript record of the "Great Survey" of much of England and parts of Wales completed in 1086 by order of King William the Conqueror.</p><h3>Why was it created?</h3><p>William needed to understand the country he had just conquered to effectively rule and tax it. The survey aimed to record:</p><ul><li>Who owned what land and property</li><li>What resources were available (ploughs, mills, livestock)</li><li>How much tax each landowner owed the King</li></ul><p>It was nicknamed the "Domesday" book by the English people because its judgements were seen as final and unappealable, like the Day of Judgement ("Doomsday").</p>' },
  { id: '2', unit: 'The Roman Empire', lessonNumber: 1, lessonTitle: 'Rise of the Empire', title: 'Rise of the Empire', type: 'Lesson', subject: 'History - Year 7-9', created: 'Oct 15, 2025', status: 'Deployed', content: '<h1>The Rise of the Roman Empire</h1><p>The Roman Empire was one of the largest and most influential empires in world history. It began in the city of Rome in 753 BC and lasted for over 1000 years.</p><h3>Key Phases of Expansion:</h3><ul><li><b>The Roman Republic (509 BC - 27 BC):</b> Rome expanded from a small city-state to control the entire Italian peninsula and then much of the Mediterranean. Key conflicts included the Punic Wars against Carthage.</li><li><b>The Early Empire (27 BC - 180 AD):</b> Under emperors like Augustus, Trajan, and Hadrian, the empire reached its greatest territorial extent, a period known as the Pax Romana (Roman Peace).</li><li><b>The Late Empire (180 AD - 476 AD):</b> The empire faced internal strife, economic problems, and external pressures, eventually leading to its division and the fall of the Western Roman Empire.</li></ul>' },
  { id: '3', unit: 'The Roman Empire', lessonNumber: 2, lessonTitle: 'Daily Life in Rome', title: 'Daily Life in Rome', type: 'Lesson', subject: 'History - Year 7-9', created: 'Oct 16, 2025', status: 'Deployed', content: '<h1>Daily Life in Ancient Rome</h1><p>Life in Rome varied greatly between the rich and the poor, but some aspects were common to many citizens.</p><h3>Housing</h3><p>Wealthy Romans lived in large, comfortable houses called a <em>domus</em>, often built around a central courtyard (atrium). The majority of the population lived in crowded apartment blocks called <em>insulae</em>, which were often poorly built and dangerous.</p><h3>Food</h3><p>The Roman diet was based on the "Mediterranean Triad": grain, grapes, and olives. Bread was a staple food, supplemented with vegetables, cheese, and some meat. The poor often relied on a free grain handout from the state, known as the <em>dole</em>.</p><h3>Entertainment</h3><p>Public entertainment was a huge part of Roman life. The Colosseum hosted gladiatorial contests and public spectacles, while the Circus Maximus was famous for its chariot races. Romans also enjoyed public baths (<em>thermae</em>), which were centers for socializing, exercising, and relaxing.</p>' },
  { id: '5', unit: 'Medieval Life', title: 'Castles and Fortifications', type: 'Lesson', subject: 'History - Year 7-9', created: 'Oct 10, 2025', status: 'Draft', content: '<h1>Medieval Castles</h1><p>Castles were a defining feature of the Middle Ages, serving as fortified homes for lords and nobles, as well as centers of administration and power.</p><h3>Early Castles: Motte and Bailey</h3><p>The first castles built by the Normans in England were of a "motte-and-bailey" design. They were quick to build using earth and timber.</p><ul><li><b>Motte:</b> A large earthen mound with a wooden tower (keep) on top.</li><li><b>Bailey:</b> A larger, enclosed area at the foot of the motte, containing barracks, stables, and workshops, protected by a wooden fence (palisade) and a ditch.</li></ul><h3>Stone Castles</h3><p>Over time, timber was replaced with stone for greater strength and fire resistance. Stone castles incorporated features like:</p><ul><li><b>The Keep:</b> The main tower and strongest point of the castle.</li><li><b>Curtain Walls:</b> The outer walls of the castle.</li><li><b>Towers and Gatehouses:</b> Positioned along the walls for defense, with the gatehouse being the heavily fortified main entrance.</li><li><b>Moats:</b> A deep, wide ditch, often filled with water, surrounding the castle.</li></ul>' },
  // Art - Textured Flat Illustration lesson with embedded image
  { 
    id: 'art-1', 
    unit: 'Digital Art Techniques', 
    lessonNumber: 1, 
    lessonTitle: 'Textured Flat Illustration', 
    title: 'Introduction to Textured Flat Illustration', 
    type: 'Lesson', 
    subject: 'Art & Design - Year 10-13', 
    created: 'Dec 5, 2025', 
    status: 'Deployed', 
    imageUrl: '/resources/test-image.jpg',
    content: '<h1>Textured Flat Illustration</h1><p>Textured flat illustration is a popular digital art style that combines the simplicity of flat design with added depth through texture overlays and grain effects.</p><h3>Key Characteristics</h3><ul><li><b>Flat Shapes:</b> Simple, geometric forms without complex gradients or 3D effects</li><li><b>Limited Color Palette:</b> Often uses a restrained set of harmonious colors</li><li><b>Texture Overlays:</b> Grain, noise, or paper textures add visual interest and depth</li><li><b>Bold Outlines:</b> Sometimes incorporates strong linework to define shapes</li></ul><h3>Techniques Used</h3><p>Artists typically create textured flat illustrations using digital tools like Adobe Illustrator for vector shapes, then add texture in Photoshop or Procreate. The texture can be applied through:</p><ul><li>Noise filters and grain effects</li><li>Paper or canvas texture overlays</li><li>Custom brushes with textured edges</li><li>Blend modes like Multiply or Overlay</li></ul><h3>Study the Example Above</h3><p>Notice how the illustration above uses flat color blocks with subtle texture to create depth. The limited palette keeps the design cohesive while the texture prevents it from looking too digital or sterile.</p>'
  },
  // Computer Science - Meta CWM Paper (PDF)
  { 
    id: 'cs-1', 
    unit: 'AI Research Papers', 
    lessonNumber: 1, 
    lessonTitle: 'Code World Models', 
    title: 'Learning Code World Models from Python Traces', 
    type: 'PDF', 
    subject: 'Computer Science - Advanced', 
    created: 'Dec 5, 2025', 
    status: 'Deployed', 
    pdfPath: '/resources/CWM Paper.pdf',
    content: '<h1>Code World Models</h1><p>This paper from Meta FAIR explores how language models can learn to simulate Python program execution by learning from execution traces.</p>'
  },
];

const initialUnitData: Omit<Unit, 'id'>[] = [
    { title: 'Norman Conquest', subject: 'History', ageRange: '10-13', description: 'A comprehensive study of the Norman conquest of England in 1066, its causes, and its consequences.' },
    { title: 'The Roman Empire', subject: 'History', ageRange: '10-13', description: 'Exploring the rise and fall of the Roman Empire, from its origins to its eventual decline.' },
    { title: 'Medieval Life', subject: 'History', ageRange: '7-9', description: 'An introduction to daily life, society, and key events during the Middle Ages.' },
    { title: 'Viking Age', subject: 'History', ageRange: '7-9', description: 'Discover the history, culture, and impact of the Vikings.' },
    { title: 'Digital Art Techniques', subject: 'Art & Design', ageRange: '14+', description: 'Modern digital illustration techniques including flat design, texturing, and color theory.' },
    { title: 'AI Research Papers', subject: 'Computer Science', ageRange: '14+', description: 'Academic papers on cutting-edge AI and machine learning research.' },
];

const initialUnits: Unit[] = initialUnitData.map((u, i) => ({ ...u, id: `unit-${i}` }));

const initialLessonFolders: LessonFolder[] = Object.values(initialContentItems.reduce((acc, item) => {
    if (item.lessonNumber && item.lessonTitle) {
      const key = `${item.unit}-${item.lessonNumber}`;
      if (!acc[key]) {
        acc[key] = {
          id: `folder-${item.unit}-${item.lessonNumber}`,
          unit: item.unit,
          number: item.lessonNumber,
          title: item.lessonTitle,
        };
      }
    }
    return acc;
  }, {} as Record<string, LessonFolder>));


const App: React.FC = () => {
  const [activeView, setActiveView] = useState<View>('lesson-library');
  const [units, setUnits] = useState<Unit[]>(initialUnits);
  const [lessonFolders, setLessonFolders] = useState<LessonFolder[]>(initialLessonFolders);
  const [contentItems, setContentItems] = useState<ContentItem[]>(initialContentItems);

  const handleCreateUnit = (unitData: Omit<Unit, 'id'>) => {
    if (!units.find(u => u.title.toLowerCase() === unitData.title.toLowerCase())) {
        const newUnit: Unit = { ...unitData, id: `unit-${Date.now()}` };
        setUnits(prev => [...prev, newUnit]);
        alert(`Unit "${unitData.title}" has been created.`);
    } else {
        alert(`Unit "${unitData.title}" already exists.`);
    }
    setActiveView('lesson-library');
  };

  const handleAddLessonFolder = (unit: string, title: string) => {
    const foldersInUnit = lessonFolders.filter(f => f.unit === unit);
    const newNumber = foldersInUnit.length > 0 ? Math.max(...foldersInUnit.map(f => f.number)) + 1 : 1;
    const newFolder: LessonFolder = {
      id: `folder-${unit}-${newNumber}-${Date.now()}`,
      unit,
      number: newNumber,
      title,
    };
    setLessonFolders(prev => [...prev, newFolder]);
    alert(`Lesson folder "${title}" has been added to unit "${unit}".`);
  };

  const handleUpdateContentItem = (updatedItem: ContentItem) => {
    setContentItems(prevItems => prevItems.map(item => item.id === updatedItem.id ? updatedItem : item));
    alert(`Content "${updatedItem.title}" has been updated.`);
  };

  const renderView = () => {
    switch (activeView) {
      case 'dashboard':
        return <ClassDashboard />;
      case 'lesson-library':
        return <LessonLibrary 
                    units={units}
                    lessonFolders={lessonFolders}
                    contentItems={contentItems} 
                    setContentItems={setContentItems} 
                    setActiveView={setActiveView} 
                    onAddLessonFolder={handleAddLessonFolder}
                    onUpdateContentItem={handleUpdateContentItem}
                />;
      case 'lesson-creator':
        return <LessonCreator onBack={() => setActiveView('lesson-library')} onSave={handleCreateUnit} />;
      case 'ai-assistant':
        return <AiAssistant />;
      case 'classroom-control':
        return <ClassroomControl 
                    units={units} 
                    lessonFolders={lessonFolders} 
                    contentItems={contentItems} 
                />;
      case 'settings':
        return <Settings />;
      default:
        return <ClassDashboard />;
    }
  };

  return (
    <div className="h-screen bg-brand-cream text-text-body font-sans selection:bg-brand-orange-light selection:text-brand-orange relative overflow-hidden">
        {/* Floating Header Wrapper - Fixed position, transparent to clicks except children */}
        <div className="absolute top-0 left-0 w-full z-50 pointer-events-none">
             <Header activeView={activeView} setActiveView={setActiveView} />
        </div>
        
        <main className="h-full overflow-y-auto bg-brand-cream scroll-smooth pt-28">
             <div className="max-w-7xl mx-auto p-8 w-full">
                 {/* Decorative Backgrounds */}
                 <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-brand-yellow rounded-full blur-3xl opacity-20 pointer-events-none -z-0 translate-x-1/3 -translate-y-1/3 mix-blend-multiply"></div>
                 <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-brand-blue rounded-full blur-3xl opacity-20 pointer-events-none -z-0 -translate-x-1/3 translate-y-1/3 mix-blend-multiply"></div>
                 
                 <div className="relative z-10">
                    {renderView()}
                 </div>
             </div>
        </main>
        <ThemeSwitcher />
    </div>
  );
};

export default App;
