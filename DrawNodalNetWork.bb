; -----------------------------------------------
; Name : Draw Nodal network V2 with root creation
; Date : (C)2025
; Site : https://github.com/BlackCreepyCat
; -----------------------------------------------

Graphics 1920,1080,0,2
SetBuffer BackBuffer()

; Définition du type NodalNet
Type NodalNet
    Field NType%        ; 0 = Point de liaison, 1 = Valeur
    Field Radius%       ; Rayon du cercle pour les nodes de valeur
    Field Parent.NodalNet ; Node parent
    Field Value#        ; Valeur du node
    Field Px%          ; Position X
    Field Py%          ; Position Y
    Field Caption$     ; Légende du node
End Type

; Variables globales pour la caméra
Global cam_zoom# = 1.0
Global cam_x# = 0
Global cam_y# = 0
Global mouse_down = False
Global last_mx%, last_my%
Global selected_node.NodalNet = Null ; Node actuellement sélectionné
Global last_selected.NodalNet = Null ; Dernier node sélectionné

; Création du réseau de test
Function CreateTestNetwork()
    ; Node racine (point de liaison)
    root.NodalNet = New NodalNet
    root\NType = 0
    root\Px = 00
    root\Py = 00
    root\Caption = "Root"
    
    ; Création de nodes de valeur mieux répartis
    For i = 1 To 15
        node.NodalNet = New NodalNet
        node\NType = 1
        node\Value = Rnd(10,100)
        node\Radius = node\Value / 2
        node\Parent = root
        node\Caption = "Node" + i
        ; Répartition en spirale pour éviter les chevauchements
        angle# = i * 24 ; Angle plus petit pour plus de points
        dist# = 50 + i * 20 ; Distance croissante
        node\Px = root\Px + Cos(angle) * dist
        node\Py = root\Py + Sin(angle) * dist
    Next
End Function

; Vérifie si un node a des enfants
Function HasChildren(node.NodalNet)
    For child.NodalNet = Each NodalNet
        If child\Parent = node Then
            Return True
        EndIf
    Next
    Return False
End Function

Function DeleteNodeAndChildren(node.NodalNet)
    ; Supprimer d'abord tous les enfants
    For child.NodalNet = Each NodalNet
        If child\Parent = node Then
            DeleteNodeAndChildren(child) ; Suppression récursive
        EndIf
    Next
    ; Supprimer le node lui-même
    Delete node
End Function

; Fonction pour vérifier si la souris est sur un node (roots inclus)
Function CheckNodeUnderMouse.NodalNet()
    mx# = (MouseX() - 400) / cam_zoom - cam_x
    my# = (MouseY() - 300) / cam_zoom - cam_y
    
    For node.NodalNet = Each NodalNet
        dx# = node\Px - mx
        dy# = node\Py - my
        dist# = Sqr(dx*dx + dy*dy)
        If node\NType = 1 Then ; Nodes de valeur
            If dist < node\Radius Then
                Return node
            EndIf
        ElseIf node\NType = 0 Then ; Roots
            If dist < 10 Then ; Rayon fixe pour les roots (ajustable)
                Return node
            EndIf
        EndIf
    Next
    Return Null
End Function

; Fonction principale
CreateTestNetwork()

While Not KeyHit(1) ; ESC pour quitter
    Cls

    ; Touche S pour sauvegarder
    If KeyHit(31) Then ; 31 = touche S
        SaveNetwork("network.txt")
    EndIf
    
    ; Touche L pour charger
    If KeyHit(38) Then ; 38 = touche L
        LoadNetwork("network.txt")
    EndIf


    
    ; Gestion du zoom avec la molette
    zoom_change = MouseZSpeed()
    If zoom_change > 0 Then cam_zoom = cam_zoom * 1.1
    If zoom_change < 0 Then cam_zoom = cam_zoom / 1.1
    If cam_zoom < 0.1 Then cam_zoom = 0.1
    
If KeyHit(211) And last_selected <> Null Then
    DeleteNodeAndChildren(last_selected) ; Supprime le node et ses enfants
    last_selected = Null ; Réinitialiser la sélection
EndIf


    ; Gestion de la souris - Clic gauche
    If MouseDown(1) Then
        If Not mouse_down Then
            mouse_down = True
            last_mx = MouseX()
            last_my = MouseY()
            
            ; Vérifier si on clique sur un node (roots ou valeur)
            selected_node = CheckNodeUnderMouse()
            If selected_node <> Null Then
                last_selected = selected_node ; Mettre à jour le dernier sélectionné
            EndIf
        Else
            If selected_node <> Null Then
                ; Déplacer le node sélectionné (fonctionne pour roots et valeur)
                mx# = (MouseX() - 400) / cam_zoom - cam_x
                my# = (MouseY() - 300) / cam_zoom - cam_y
                selected_node\Px = mx
                selected_node\Py = my
            Else
                ; Déplacer la caméra
                cam_x = cam_x + (MouseX() - last_mx) / cam_zoom
                cam_y = cam_y + (MouseY() - last_my) / cam_zoom
                last_mx = MouseX()
                last_my = MouseY()
            EndIf
        EndIf
    Else
        mouse_down = False
        selected_node = Null
    EndIf
    
; Gestion du clic droit - Ajout d'un nouveau node
If MouseHit(2) And last_selected <> Null Then
    new_node.NodalNet = New NodalNet
    new_node\NType = 1
    new_node\Value = Rnd(10,100)
    new_node\Radius = new_node\Value / 2
    new_node\Parent = last_selected
    new_node\Caption = "New" + Rnd(1,1000)

    ; Positionner le nouveau node à la position de la souris dans le monde
    new_node\Px = (MouseX() - 400) / cam_zoom - cam_x
    new_node\Py = (MouseY() - 300) / cam_zoom - cam_y
EndIf
    
; Gestion du clic milieu - Ajout d'un nouveau root et mise à jour du root en cours
If MouseHit(3) Then
    new_root.NodalNet = New NodalNet
    new_root\NType = 0
    new_root\Px = (MouseX() - 400) / cam_zoom - cam_x
    new_root\Py = (MouseY() - 300) / cam_zoom - cam_y
    new_root\Caption = "Root" + Rnd(1,1000)
    last_selected = new_root ; Définit le nouveau root comme le dernier sélectionné
EndIf

    ; Dessin du réseau
    For node.NodalNet = Each NodalNet
        screen_x# = (node\Px + cam_x) * cam_zoom + 400
        screen_y# = (node\Py + cam_y) * cam_zoom + 300
        
        ; Dessin des lignes vers le parent
        If node\Parent <> Null Then
            parent_x# = (node\Parent\Px + cam_x) * cam_zoom + 400
            parent_y# = (node\Parent\Py + cam_y) * cam_zoom + 300
            Color 100,100,100
            Line screen_x, screen_y, parent_x, parent_y
        EndIf
        
        ; Dessin des nodes
        If node\NType = 0 Then
            ; Dessin du root
            Color 255,0,0
            Plot screen_x, screen_y
            Oval screen_x-3, screen_y-3, 6, 6, 0
            
            ; Ajout du cercle rouge foncé si sélectionné
            If node = selected_node Or node = last_selected Then
                Color 139,0,0 ; Rouge foncé
                radius_scaled# = 10 * cam_zoom ; Rayon fixe adapté au zoom
                Oval screen_x - radius_scaled, screen_y - radius_scaled,    radius_scaled * 2, radius_scaled * 2, 0
            EndIf
            
            Color 255,255,255
            Text screen_x, screen_y+10, node\Caption, True, False
        Else
            ; Dessin des nodes de valeur (inchangé)
            radius_scaled# = node\Radius * cam_zoom
            If node = selected_node Then
                Color 0,255,0 ; Vert si actuellement sélectionné
            ElseIf node = last_selected Then
                Color 0,255,0 ; Vert si dernier sélectionné
            ElseIf HasChildren(node) Then
                Color 255,165,0 ; Orange si le node a des enfants
            Else
                Color 0,0,255 ; Bleu sinon
            EndIf
            
            Oval screen_x - radius_scaled, screen_y - radius_scaled,   radius_scaled * 2, radius_scaled * 2, 0
            Color 255,255,255
            Text screen_x, screen_y+20, node\Caption, True, False
            Text screen_x, screen_y, Int(node\Value), True, True
        EndIf
    Next

    
    ; Affichage des infos
    Color 255,255,255
    Text 10,10, "Zoom: "+cam_zoom
    Text 10,25, "Molette: zoom | Gauche: déplacer | Droit: ajouter node"
    
    Flip
Wend

End



; Fonction pour sauvegarder les nodes dans un fichier texte
Function SaveNetwork(filename$)
    file = WriteFile(filename$)
    If file = 0 Then Return False
    
    ; Compter et écrire le nombre total de nodes
    count = 0
    For node.NodalNet = Each NodalNet
        count = count + 1
    Next
    WriteInt(file, count)
    
    ; Écrire chaque node avec ses données
    For node.NodalNet = Each NodalNet
        WriteInt(file, node\NType)       ; Type de node
        WriteInt(file, node\Radius)      ; Rayon
        WriteFloat(file, node\Value)     ; Valeur
        WriteInt(file, node\Px)          ; Position X
        WriteInt(file, node\Py)          ; Position Y
        WriteString(file, node\Caption)  ; Légende
        
        ; Trouver et écrire l'index du parent
        parent_index = -1
        index = 0
        For n.NodalNet = Each NodalNet
            If n = node\Parent Then
                parent_index = index
                Exit
            EndIf
            index = index + 1
        Next
        WriteInt(file, parent_index)
    Next
    
    CloseFile(file)
    Return True
End Function

; Fonction pour charger les nodes depuis un fichier texte
Function LoadNetwork(filename$)
    ; Supprimer tous les nodes existants
    For node.NodalNet = Each NodalNet
        Delete node
    Next
    
    file = ReadFile(filename$)
    If file = 0 Then Return False
    
    ; Lire le nombre de nodes
    count = ReadInt(file)
    
    ; Première passe : créer tous les nodes sans leurs parents
    For i = 1 To count
        node.NodalNet = New NodalNet
        node\NType = ReadInt(file)
        node\Radius = ReadInt(file)
        node\Value = ReadFloat(file)
        node\Px = ReadInt(file)
        node\Py = ReadInt(file)
        node\Caption = ReadString(file)
        ReadInt(file) ; Sauter l'index du parent pour l'instant
    Next
    
    ; Remettre le pointeur au début après le count
    SeekFile(file, 4)
    
    ; Deuxième passe : assigner les parents
    For node.NodalNet = Each NodalNet
        ReadInt(file)    ; Sauter NType
        ReadInt(file)    ; Sauter Radius
        ReadFloat(file)  ; Sauter Value
        ReadInt(file)    ; Sauter Px
        ReadInt(file)    ; Sauter Py
        ReadString(file) ; Sauter Caption
        parent_index = ReadInt(file)
        
        If parent_index >= 0 Then
            ; Trouver le parent par son index
            index = 0
            For n.NodalNet = Each NodalNet
                If index = parent_index Then
                    node\Parent = n
                    Exit
                EndIf
                index = index + 1
            Next
        Else
            node\Parent = Null
        EndIf
    Next
    
    CloseFile(file)
    Return True
End Function
